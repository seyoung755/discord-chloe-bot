package com.example.discordbotspring;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

public class MessageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String BOT_NAME = "Chloe-bot";
    private static JDA jda;
    private static TimerTask checkInTask;
    private static TimerTask checkOutTask;

    public static void run() throws LoginException {

        String token = System.getenv("TOKEN");
        jda = JDABuilder.createDefault(token).build();
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.addEventListener(new MessageListener());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (isValidRequest(event)) {
            TextChannel textChannel = event.getGuild().getTextChannelsByName("✅체크인-체크아웃", true).get(0);

            textChannel.sendMessage("체크인/체크아웃 알림을 시작합니다.").queue();

            Calendar calendar = Calendar.getInstance();

            checkInTask = new TimerTask(){
                public void run(){

                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

                    if (isWeekend(dayOfWeek)) {
                        logger.info("주말은 쉬어야죠", dayOfWeek);
                        return;
                    }

                    LocalDateTime now = LocalDateTime.now();

                    String dateString = now.toString();
                    String month = dateString.substring(5, 7);
                    String day = dateString.substring(8, 10);
                    textChannel.sendMessage(month + "월 " + day + "일 체크인 시간입니다~ 스레드에 체크인 댓글을 남겨주세요!").queue();
                }
            };

            checkOutTask = new TimerTask() {
                @Override
                public void run() {
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

                    if (isWeekend(dayOfWeek)) {
                        logger.info("주말은 쉬어야죠", dayOfWeek);
                        return;
                    }

                    LocalDateTime now = LocalDateTime.now();

                    String dateString = now.toString();
                    String month = dateString.substring(5, 7);
                    String day = dateString.substring(8, 10);
                    textChannel.sendMessage(month + "월 " + day + "일 체크아웃 시간입니다~ 스레드에 체크아웃 댓글을 남겨주세요!").queue();
                }
            };

            Date checkIn = null;
            Date checkOut = null;
            LocalDateTime runtime = LocalDateTime.now();

            try {
                checkIn = dateFormatter.parse(runtime.toString().substring(0, 10) + " 02:22:30");
                checkOut = dateFormatter.parse(runtime.toString().substring(0, 10) + " 18:00:00");
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //Now create the time and schedule it
            Timer timer = new Timer();

            //Use this if you want to execute it once
            timer.scheduleAtFixedRate(checkInTask, checkIn, 1000*60*60*24);
            timer.scheduleAtFixedRate(checkOutTask, checkOut, 1000*60*60*24);
        }
    }

    private boolean isWeekend(int dayOfWeek) {
        return dayOfWeek == 1 || dayOfWeek == 7;
    }

    private boolean isValidRequest(@NotNull MessageReceivedEvent event) {
        return isNotBotsMessage(event) && isStartMessage(event) && isFirstCommand(checkInTask);
    }

    private boolean isStartMessage(MessageReceivedEvent event) {
        return event.getMessage().getContentRaw().equals("!클로이봇 시작");
    }

    private boolean isFirstCommand(TimerTask task) {
        return Objects.isNull(task);
    }


    private boolean isNotBotsMessage(@NotNull MessageReceivedEvent event) {
        return !event.getAuthor().getName().equals(BOT_NAME);
    }

//    private long calcTaskTime(int startTime) {
//
//        if(startTime > 23 || startTime < 0){
//            return 0;
//        }
//        Calendar calendar = new GregorianCalendar(Locale.KOREA);
//        calendar.set(Calendar.HOUR_OF_DAY, startTime);
//        calendar.set(Calendar.MINUTE, 6);
//        calendar.set(Calendar.SECOND, 0);
//
//        long nowDate = new Date().getTime();
//
//        if (nowDate > calendar.getTime().getTime()) {
//            calendar.add(Calendar.DAY_OF_YEAR, 1);
//        }
//        long waiting = (calendar.getTime().getTime() - nowDate)/1000;
//        logger.info("Schedule Start Time : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime()));
//        logger.info("Waiting : " + waiting+" sec");
//
//        return (int)waiting;
//    }
}
