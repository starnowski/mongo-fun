package com.github.starnowski.mongo.fun.services;

import com.github.starnowski.mongo.fun.AbstractITTest;
import com.github.starnowski.mongo.fun.model.Comment;
import com.github.starnowski.mongo.fun.model.Post;
import com.github.starnowski.mongo.fun.repositories.CommentDao;
import com.github.starnowski.mongo.fun.repositories.PostDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PostServiceTest extends AbstractITTest {

    @Autowired
    CommentDao commentDao;

    @Autowired
    PostDao postDao;

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
        assertNotNull(result.getId());
        result = tested.find(result.getOid());
        assertEquals(text, result.getText());
        assertEquals(email, result.getEmail());
        Assertions.assertTrue(result.getComments().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("provide_shouldCreatePostWithComments")
    public void shouldCreatePostWithCommentsThatAreStoredInOtherCollection(Post post) {
        // WHEN
        Post result = tested.save(post);

        // THEN
        assertNotNull(result.getId());
        result = tested.find(result.getOid());
        assertEquals(post.getText(), result.getText());
        assertEquals(post.getEmail(), result.getEmail());
        assertNotNull(result.getComments());
        assertFalse(result.getComments().isEmpty());
        assertEquals(post.getComments().size(), result.getComments().size());
        assertIterableEquals(extractCommentsTextContents(post.getComments()), extractCommentsTextContents(result.getComments()));
        result.getComments().forEach(comment ->
        {

            assertNotNull(comment.getId());
            Comment entity = commentDao.find(comment.getId());
            assertNotNull(entity);
            assertEquals(comment.getEmail(), entity.getEmail());
            assertEquals(comment.getText(), entity.getText());
        });
    }

    @ParameterizedTest
    @MethodSource("provide_shouldCreatePostWithComments")
    public void shouldCreatePostWithCommentsThatAreNotStoredAsNestedArray(Post post) {
        // WHEN
        Post result = tested.save(post);

        // THEN
        assertNotNull(result.getId());
        result = postDao.find(result.getOid());
        assertEquals(post.getText(), result.getText());
        assertEquals(post.getEmail(), result.getEmail());
        assertNull(result.getComments());
    }

    private Set<String> extractCommentsTextContents(List<Comment> comments) {
        return comments.stream().map(Comment::getText).collect(Collectors.toSet());
    }
}