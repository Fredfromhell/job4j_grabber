package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Grabber.class.getName());

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(
                    cfg.getProperty("url"),
                    cfg.getProperty("username"),
                    cfg.getProperty("password"));
        } catch (Exception e) {
            LOG.error("Ошибка соединения", e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement preparedStatement =
                     cnn.prepareStatement("insert into post (name, text"
                                     + ", link, created) values(?,?,?,?)"
                                     + " on conflict (link) do nothing ",
                             Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, post.getTitle());
            preparedStatement.setString(2, post.getDescription());
            preparedStatement.setString(3, post.getLink());
            preparedStatement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            preparedStatement.execute();
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            LOG.error("Ошибка выполнения запроса save", e);
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try (PreparedStatement preparedStatement =
                     cnn.prepareStatement("select * from post")) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(queryPost(resultSet));
                }
            }
        } catch (SQLException e) {
            LOG.error("Ошибка выполнения запроса getAll", e);
        }
        return list;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement preparedStatement =
                     cnn.prepareStatement("select * from post where id = ?")) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    post = queryPost(resultSet);
                }
            }
        } catch (SQLException e) {
            LOG.error("Ошибка выполнения запроса findById", e);
        }
        return post;
    }

    public Post queryPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("text"),
                (resultSet.getTimestamp("created")).toLocalDateTime());
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader().
                getResourceAsStream("grabber.properties")) {
            config.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> list =
                habrCareerParse.list("https://career.habr.com/vacancies/java_developer?page=");
        try (PsqlStore psqlStore = new PsqlStore(config)) {
            for (Post post : list) {
                psqlStore.save(post);
                System.out.println(psqlStore.getAll());
                System.out.println(psqlStore.findById(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}