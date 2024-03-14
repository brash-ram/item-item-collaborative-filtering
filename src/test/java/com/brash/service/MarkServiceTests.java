package com.brash.service;

import com.brash.FilterApplication;
import com.brash.IntegrationEnvironment;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.MarkRepository;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FilterApplication.class)
@Import(IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MarkServiceTests {

    @Autowired
    private MarkService markService;

    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private MarkRepository markRepository;

    @Test
    @Transactional
    @Rollback
    public void getGeneratedMarksTest() {
        User user = userService.addUser(1L);
        Item item = itemService.addItem(1L);
        markRepository.save(new Mark().setItem(item).setUser(user).setMark(1.0).setIsGenerated(true));
        markRepository.save(new Mark().setItem(item).setUser(user).setMark(5.0).setIsGenerated(true));

        List<Mark> marks = null;
        marks = markService.getGeneratedMarks(1L);

        assertNotNull(marks);
        assertEquals(1, marks.size());
        assertEquals(5.0, marks.get(0).getMark(), 0.1);
    }
}
