package com.brash.hard.random;

import com.brash.FilterApplication;
import com.brash.IntegrationEnvironment;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.filter.Filter;
import com.brash.hard.RandomUtils;
import com.brash.service.FilterScheduler;
import org.junit.jupiter.api.MethodOrderer;
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
import java.util.List;

import static com.brash.hard.random.RandomHardTestSettings.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FilterApplication.class)
@Import(IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@MockBean(FilterScheduler.class)
public class RandomHardTests {

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private Filter filter;

    private int numberUserMark;
    
    private void saveTestData() {
        RandomUtils randomUtils = new RandomUtils();
        List<User> users = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < NUMBER_USERS; i++) {
            users.add(userRepository.save(new User().setOriginalId((long) randomUtils.getRandomInt(1, 1000000000))));
        }
        for (int i = 0; i < NUMBER_ITEMS; i++) {
            items.add(itemRepository.save(new Item().setOriginalId((long) randomUtils.getRandomInt(1, 1000000000))));
        }
        
        int percentMarkedItems = (int) (NUMBER_ITEMS * (MAX_PERCENT_MARKED_ITEMS / 100.0));
        for (User user : users) {
            List<Item> markedItems = new ArrayList<>();
            for (int i = 0; i < percentMarkedItems; i++) {
                int mark = randomUtils.getRandomInt(MIN_MARK, MAX_MARK);
                Item item = randomUtils.getRandomUniqueItem(items, markedItems);
                markedItems.add(item);
                markRepository.save(new Mark().setUser(user).setItem(item).setMark((double) mark));
                numberUserMark++;
            }

        }
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    @Test
    @Transactional
    @Rollback
    public void randomHardTest() {
        saveTestData();

        System.out.println("Начинается генерация рекомендаций");
        long start = System.currentTimeMillis();
        filter.updateRecommendations();
        long end = System.currentTimeMillis();

        TestTransaction.flagForCommit();
        TestTransaction.end();

        int numberGeneratedMarks = markRepository.countAllByIsGenerated(true);
        List<Mark> zeroMarks = markRepository.findAllByMarkLessThan(0.001);

        System.out.println("Время работы генерации рекомендаций: " + ((end - start) / 1000.0) + " сек");
        System.out.println("Размер матрицы оценок: " + NUMBER_USERS * NUMBER_ITEMS);
        System.out.println("Поставлено оценок - " + numberUserMark);
        System.out.println("Матрица оценок заполнена пользователями на " + ((double) numberUserMark / (NUMBER_USERS * NUMBER_ITEMS)  * 100.0) + "%");
        System.out.println("Количество сгенерированных оценок: " + numberGeneratedMarks);
        System.out.println("Матрица оценок заполнена алгоритмом на " + ((double) numberGeneratedMarks / (NUMBER_USERS * NUMBER_ITEMS - numberUserMark)  * 100.0) + "%");
        System.out.println("Матрица заполнена на " + ((double) (numberGeneratedMarks + numberUserMark) / (NUMBER_USERS * NUMBER_ITEMS)  * 100.0) + "%");
        System.out.println("Количество нулевых оценок: " + zeroMarks.size());
    }
}
