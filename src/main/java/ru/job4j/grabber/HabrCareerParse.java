package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private static final Logger LOG = LoggerFactory.getLogger(Grabber.class.getName());

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String
            .format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private static final int NUMBER_OF_PAGES = 5;

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private String retrieveDescription(String link) {
        Connection connection = Jsoup.connect(link);
        Document document = null;
        try {
            document = connection.get();
        } catch (IOException e) {
            LOG.error("Ошибка парсинга описания", e);
        }
        Elements row = document.select(".style-ugc");
        return row.text();

    }

    @Override
    public List<Post> list(String link) {
        List<Post> newList = new ArrayList<>();
        for (int i = 1; i <= NUMBER_OF_PAGES; i++) {
            Connection connection = Jsoup.connect(link + i);
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                LOG.error("Ошибка парсига страницы", e);
            }
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                newList.add(parse(row));

            });

        }
        return newList;
    }

    private Post parse(Element row) {
        Element titleElement = row.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        Element dateElement = row.select(".vacancy-card__date").first();
        String vacancyName = titleElement.text();
        Element date1 = dateElement.child(0);
        String date = date1.attr("datetime");
        String link1 = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        return new Post(vacancyName, link1, retrieveDescription(link1),
                dateTimeParser.parse(date));

    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        System.out.println(habrCareerParse.list(PAGE_LINK));
    }

}