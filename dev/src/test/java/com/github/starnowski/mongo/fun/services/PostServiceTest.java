package com.github.starnowski.mongo.fun.services;

import com.github.starnowski.mongo.fun.model.Comment;
import com.github.starnowski.mongo.fun.model.Post;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.stream.Stream;

@SpringBootTest
class PostServiceTest {

    @Autowired
    PostService tested;

    private static Stream<Arguments> provide_shouldCreatePostWithComments() {
        return Stream.of(
                Arguments.of(new Post().withEmail("john.doe.21@gmail.com").withText("post1 test")
                        .withComments(Arrays.asList(new Comment().withEmail("gosia.talarz.14@interia.pl").withText("comment1 post1"),
                                new Comment().withEmail("magie.talarz.123@interia.pl").withText("comment2 post1")))),
                Arguments.of(new Post().withEmail("mike.doe.21@gmail.com").withText("post2 test")
                        .withComments(Arrays.asList(new Comment().withEmail("bill.doe.14@interia.pl").withText("comment1 post2"))))
        );
    }

    @Test
    public void shouldCreatePost() {
        // GIVEN
        String email = "szymon.doe@gmail.com";
        String text = "Some text 1";
        Post post = new Post();
        post.setEmail(email);
        post.setText(text);

        // WHEN
        Post result = tested.save(post);

        // THEN
        Assertions.assertNotNull(result.getId());
        result = tested.find(result.getOid());
        Assertions.assertEquals(text, result.getText());
        Assertions.assertEquals(email, result.getEmail());
        Assertions.assertTrue(result.getComments() == null);
    }

    @ParameterizedTest
    @MethodSource("provide_shouldCreatePostWithComments")
    public void shouldCreatePostWithCommentsThatAreStoredInOtherCollection(Post post) {
        // WHEN
        Post result = tested.save(post);

        // THEN
        Assertions.assertNotNull(result.getId());
        result = tested.find(result.getOid());
        Assertions.assertEquals(post.getText(), result.getText());
        Assertions.assertEquals(post.getEmail(), result.getEmail());
        Assertions.assertNotNull(result.getComments());
        Assertions.assertFalse(result.getComments().isEmpty());
        Assertions.assertEquals(post.getComments().size(), result.getComments().size());
        //TODO Compare comments text
    }
}