package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;

public class AlertRabbit {

    public static Properties readProperties() {
        Properties config = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader().
                getResourceAsStream("rabbit.properties")) {
            config.load(in);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return config;
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Properties config = readProperties();
        int repeatTime = Integer.parseInt(config.
                getProperty("rabbit.interval"));
        Class.forName(config.getProperty("jdbc.driver"));
        try (Connection connect = DriverManager.getConnection(
                config.getProperty("jdbc.url"),
                config.getProperty("jdbc.username"),
                config.getProperty("jdbc.password")
        )) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connect", connect);
            JobDetail job = newJob(Rabbit.class).usingJobData(data).build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(repeatTime)
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        } catch (InterruptedException | SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static class Rabbit implements Job {
        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection connect = (Connection) context
                    .getJobDetail()
                    .getJobDataMap()
                    .get("connect");
            try (PreparedStatement ps = connect.prepareStatement(
                    "insert into rabbit(created_date) values (?);")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}