package com.brash.data;

import com.brash.FilterApplication;
import com.brash.IntegrationEnvironment;
import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FilterApplication.class)
@Import(IntegrationEnvironment.JpaIntegrationEnvironmentConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MarkRepositoryTests {

    @Autowired
    private MarkRepository markRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @Transactional
    @Rollback
    public void testFindAllMarks() {
        Item item = itemRepository.saveAndFlush(new Item().setOriginalId(2L));
        User user = userRepository.saveAndFlush(new User().setOriginalId(2L));
        Mark mark = markRepository.saveAndFlush(new Mark(null, user, item, 5.0, false));

        Item savedItem = itemRepository.findById(item.getId()).get();
        User savedUser = userRepository.findById(user.getId()).get();

        assertEquals(1, savedItem.getMarks().size());
        assertEquals(1, savedUser.getMarks().size());
    }
}
