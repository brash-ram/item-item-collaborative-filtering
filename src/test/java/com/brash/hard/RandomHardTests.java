package com.brash.hard;

import com.brash.FilterApplication;
import com.brash.IntegrationEnvironment;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.filter.Filter;
import com.brash.filter.ItemToItemSimilarity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import java.util.List;

import static com.brash.hard.RandomHardTestSettings.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FilterApplication.class)
@Import(IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        System.out.println("Поставлено оценок - " + numberUserMark);
    }

    @Test
    @Transactional
    @Rollback
    public void randomHardTest() {
        saveTestData();

        long start = System.currentTimeMillis();
        filter.updateRecommendations();
        long end = System.currentTimeMillis();

        System.out.println("Время работы генерации рекомендаций: " + ((end - start) / 1000.0) + " сек");

        TestTransaction.flagForCommit();
        TestTransaction.end();

        List<Mark> marks = markRepository.findAllByIsGenerated(true);
        int numberMarkWithZero = 0;
        for (Mark mark : marks) {
            if (mark.getMark() < 0.001) {
                numberMarkWithZero++;
            }
        }
        System.out.println("Количество сгенерированных оценок: " + marks.size());
        System.out.println("Размер матрицы оценок: " + NUMBER_USERS * NUMBER_ITEMS);
        System.out.println("Алгоритм заполнил матрицу на : " + ((double) marks.size() / (NUMBER_USERS * NUMBER_ITEMS - numberUserMark)  * 100.0) + "%");
        System.out.println("Количество нулевых оценок: " + numberMarkWithZero);
    }
}
