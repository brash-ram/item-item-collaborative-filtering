package com.brash.filter;

import com.brash.FilterApplication;
import com.brash.IntegrationEnvironment;
import com.brash.data.entity.HavingMarks;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.filter.data.SimilarItems;
import com.brash.service.FilterScheduler;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Класс тестов для алгоритма совместной фильтрации
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FilterApplication.class)
@Import(IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MockBean(FilterScheduler.class)
public class FilterTests {

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemToItemSimilarity itemToItemSimilarity;

    @Autowired
    private Filter filter;
    private static final List<User> TEST_USERS = new ArrayList<>();
    private static final List<Item> TEST_ITEMS = new ArrayList<>();

    /**
     * Загрузка тестовой матрицы оценок.
     * Просчет примера https://www.geeksforgeeks.org/item-to-item-based-collaborative-filtering/
     */
    public void saveData() {
        for (long i = 0L; i < 4; i++) {
            TEST_USERS.add(userRepository.save(new User().setOriginalId(i)));
        }
        for (long i = 0L; i < 3; i++) {
            TEST_ITEMS.add(itemRepository.save(new Item().setOriginalId(i)));
        }
        markRepository.save(new Mark(null, TEST_USERS.get(0), TEST_ITEMS.get(1), 2.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(0), TEST_ITEMS.get(2), 3.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(1), TEST_ITEMS.get(0), 5.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(1), TEST_ITEMS.get(1), 2.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(2), TEST_ITEMS.get(0), 3.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(2), TEST_ITEMS.get(1), 3.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(2), TEST_ITEMS.get(2), 1.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(3), TEST_ITEMS.get(1), 2.0, false));
        markRepository.save(new Mark(null, TEST_USERS.get(3), TEST_ITEMS.get(2), 2.0, false));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    /**
     * Расчет значений пар сходства
     */
    @Test
    @Transactional
    @Rollback
    @Order(1)
    public void itemItemSimilarityCalculateTest() throws InterruptedException {
        saveData();
        List<Item> items = itemRepository.findAll();
        List<HavingMarks> havingMarksItems = items.stream().map(item -> (HavingMarks)item).toList();
        List<SimilarItems> part = itemToItemSimilarity.updateSimilarity(havingMarksItems);

        assertEquals(3, part.size());

        part.sort(Comparator.comparingDouble(o -> o.similarValue));

        assertEquals(0.47, part.get(0).similarValue, 0.01);
        assertEquals(0.54, part.get(1).similarValue, 0.01);
        assertEquals(0.79, part.get(2).similarValue, 0.01);

        TestTransaction.flagForRollback();
        TestTransaction.end();
    }

    /**
     * Просчет значений оценок рекомендаций
     */
    @Test
    @Transactional
    @Rollback
    @Order(2)
    public void filterTest() {
        filter.updateRecommendations();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        List<Mark> marks = markRepository.findAll();
        assertEquals(12, marks.size());

        List<Mark> markUserUser1Item2 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(0)) &&
                                mark.getItem().equals(TEST_ITEMS.get(1)))
                .toList();
        assertEquals(1, markUserUser1Item2.size());
        assertEquals(2.0, markUserUser1Item2.get(0).getMark(), 0.1);

        List<Mark> markUserUser2Item3 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(1)) &&
                                mark.getItem().equals(TEST_ITEMS.get(2)))
                .toList();
        assertEquals(1, markUserUser2Item3.size());
        assertEquals(2.21, markUserUser2Item3.get(0).getMark(), 0.1);

        List<Mark> markUserUser4Item1 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(3)) &&
                                mark.getItem().equals(TEST_ITEMS.get(0)))
                .toList();
        assertEquals(1, markUserUser4Item1.size());
        assertEquals(3.86, markUserUser4Item1.get(0).getMark(), 0.1);
    }

    /**
     * Повторный просчет оценок для тестирования
     * не только генерации новых значений, но и обновления существующих.
     * Оценки сгенерированные в тесте Order(2) сохраняются в бд для этого теста.
     */
    @Test
    @Transactional
    @Rollback
    @Order(3)
    public void repeatedFilterTest() {
        filter.updateRecommendations();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        List<Mark> marks = markRepository.findAll();
        assertEquals(12, marks.size());

        List<Mark> markUserUser1Item2 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(0)) &&
                                mark.getItem().equals(TEST_ITEMS.get(1)))
                .toList();
        assertEquals(1, markUserUser1Item2.size());
        assertEquals(2.0, markUserUser1Item2.get(0).getMark(), 0.1);

        List<Mark> markUserUser2Item3 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(1)) &&
                                mark.getItem().equals(TEST_ITEMS.get(2)))
                .toList();
        assertEquals(1, markUserUser2Item3.size());
        assertEquals(2.21, markUserUser2Item3.get(0).getMark(), 0.1);

        List<Mark> markUserUser4Item1 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(3)) &&
                                mark.getItem().equals(TEST_ITEMS.get(0)))
                .toList();
        assertEquals(1, markUserUser4Item1.size());
        assertEquals(3.86, markUserUser4Item1.get(0).getMark(), 0.1);
    }

}
