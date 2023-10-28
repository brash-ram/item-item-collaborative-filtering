package com.brash.filter;

import com.brash.FilterApplication;
import com.brash.IntegrationEnvironment;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FilterApplication.class)
@Import(IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    public void saveData() {
        for (long i = 0L; i < 4; i++) {
            TEST_USERS.add(userRepository.saveAndFlush(new User().setOriginalId(i)));
        }
        for (long i = 0L; i < 3; i++) {
            TEST_ITEMS.add(itemRepository.saveAndFlush(new Item().setOriginalId(i)));
        }
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(0), TEST_ITEMS.get(0), 2.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(0), TEST_ITEMS.get(2), 3.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(1), TEST_ITEMS.get(0), 5.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(1), TEST_ITEMS.get(1), 2.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(2), TEST_ITEMS.get(0), 3.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(2), TEST_ITEMS.get(1), 3.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(2), TEST_ITEMS.get(2), 1.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(3), TEST_ITEMS.get(1), 2.0, false));
        markRepository.saveAndFlush(new Mark(null, TEST_USERS.get(3), TEST_ITEMS.get(2), 2.0, false));
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    @Test
    @Transactional
    @Rollback
    @Order(1)
    public void itemItemSimilarityCalculateTest() {
        saveData();
        List<Item> items = itemRepository.findAll();
        List<PartSimilarItems> part = itemToItemSimilarity.updateSimilarity(items);

        assertEquals(3, part.size());

        part.sort(Comparator.comparingDouble(o -> o.similarValue));

        assertEquals(0.789, part.get(0).similarValue, 0.01);
        assertEquals(0.869, part.get(1).similarValue, 0.01);
        assertEquals(0.9, part.get(2).similarValue, 0.1);
        TestTransaction.flagForRollback();
        TestTransaction.end();
    }

    @Test
    @Transactional
    @Rollback
    @Order(2)
    public void itemItemRecommendationCalculateTest() {
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
        assertEquals(2.49, markUserUser1Item2.get(0).getMark(), 0.1);

        List<Mark> markUserUser2Item3 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(1)) &&
                                mark.getItem().equals(TEST_ITEMS.get(2)))
                .toList();
        assertEquals(1, markUserUser2Item3.size());
        assertEquals(3.43, markUserUser2Item3.get(0).getMark(), 0.1);

        List<Mark> markUserUser4Item1 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(3)) &&
                                mark.getItem().equals(TEST_ITEMS.get(0)))
                .toList();
        assertEquals(1, markUserUser4Item1.size());
        assertEquals(2.0, markUserUser4Item1.get(0).getMark(), 0.1);
    }

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
        assertEquals(2.49, markUserUser1Item2.get(0).getMark(), 0.1);

        List<Mark> markUserUser2Item3 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(1)) &&
                                mark.getItem().equals(TEST_ITEMS.get(2)))
                .toList();
        assertEquals(1, markUserUser2Item3.size());
        assertEquals(3.43, markUserUser2Item3.get(0).getMark(), 0.1);

        List<Mark> markUserUser4Item1 = marks.stream()
                .filter(mark ->
                        mark.getUser().equals(TEST_USERS.get(3)) &&
                                mark.getItem().equals(TEST_ITEMS.get(0)))
                .toList();
        assertEquals(1, markUserUser4Item1.size());
        assertEquals(2.0, markUserUser4Item1.get(0).getMark(), 0.1);
    }

}
