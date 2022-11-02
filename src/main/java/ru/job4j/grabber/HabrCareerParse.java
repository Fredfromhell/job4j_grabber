package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String
            .format("%s/vacancies/java_developer", SOURCE_LINK);

    private static final int NUMBER_OF_PAGES = 1;

    private static String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Elements row = document.select(".style-ugc");
        return row.text();

    }

  /*
  public static void main(String[] args) throws IOException {
        for (int i = 1; i <= NUMBER_OF_PAGES; i++) {
            Connection connection = Jsoup.connect(PAGE_LINK + "?page=" + i);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element dateElement = row.select(".vacancy-card__date").first();
                String vacancyName = titleElement.text();
                Element date1 = dateElement.child(0);
                String date = date1.attr("datetime");
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                HabrCareerDateTimeParser validDate = new HabrCareerDateTimeParser();
                System.out.printf("%s %s %s%n", vacancyName, link, validDate.parse(date));
            });
        }
    }
    */

    @Override
    public List<Post> list(String link) {
        AtomicInteger id = new AtomicInteger(1);
        List<Post> newList = new ArrayList<>();
        for (int i = 1; i <= NUMBER_OF_PAGES; i++) {
            Connection connection = Jsoup.connect(link + "?page=" + i);
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element dateElement = row.select(".vacancy-card__date").first();
                String vacancyName = titleElement.text();
                Element date1 = dateElement.child(0);
                String date = date1.attr("datetime");
                String link1 = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                HabrCareerDateTimeParser habrCareerDateTimeParser = new HabrCareerDateTimeParser();
                try {
                    newList.add(new Post(id.get(), vacancyName, link1, retrieveDescription(link1),
                            habrCareerDateTimeParser.parse(date)));
                    id.getAndIncrement();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        }
        return newList;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse();
        System.out.println(habrCareerParse.list(PAGE_LINK));
    }

}