package com.github.starnowski.mongo.fun.repositories;

import com.github.starnowski.mongo.fun.model.Comment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommentDaoTest {

    @Autowired
    CommentDao commentDao;


    @ParameterizedTest
    @ValueSource(strings = {"comment1", "comment2", "commentFinal"})
    public void shouldSavePost(String commentText) {
        // GIVEN
        Comment comment = new Comment();
        String email = "john2001.doe@gmailcom";
        comment.setEmail(email);
        comment.setText(commentText);

        // WHEN
        Comment result = commentDao.save(comment);

        // THEN
        result = commentDao.find(result.getOid());
        Assertions.assertNotNull(result.getId());
        Assertions.assertEquals(commentText, result.getText());
        Assertions.assertEquals(email, result.getEmail());
    }

}