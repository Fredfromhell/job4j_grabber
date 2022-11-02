package ru.job4j.grabber;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class HabrCareerDateTimeParser implements DateTimeParser {

    @Override
    public LocalDateTime parse(String parse) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(parse);
        return zonedDateTime.toLocalDateTime();
    }

}