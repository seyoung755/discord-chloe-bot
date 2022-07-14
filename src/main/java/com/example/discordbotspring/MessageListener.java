package com.example.discordbotspring;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.dv8tion.jda.api.entities.Activity.ActivityType.*;

public class MessageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
    private static final String CHECK_OUT_MODE = "체크아웃";
    private static final String CHECK_IN_MODE = "체크인";
    private static final String BOT_NAME = "Chloe-bot";
    private static final int ONE_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;
    private static final ConcurrentMap<String, String> guilds = new ConcurrentHashMap<>();
    private static final String BOT_START_COMMAND = "!클로이봇 시작";
    private static final String BOT_STOP_COMMAND = "!클로이봇 종료";
    private static final String STATUS_COMMAND = "!상태";
    private static final String NEXT_ALARM_COMMAND = "!다음 알림";

    private final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static JDA jda;
    private Date checkIn;
    private Date checkOut;

    public static void run() throws LoginException {
        String token = System.getenv("TOKEN");
        jda = JDABuilder.createDefault(token).build();
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.addEventListener(new MessageListener());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!isRegistered(event.getGuild())) {
            runChloeBot(event);
        }
        switch (event.getMessage().getContentRaw()) {
            case STATUS_COMMAND:
                checkStatus(event);
                break;

            case BOT_STOP_COMMAND:
                stopChloeBot(event);
                break;

            case NEXT_ALARM_COMMAND:
                sendNextAlarmTime(event);
                break;

            default:
                // do nothing
        }
    }

    private void sendNextAlarmTime(MessageReceivedEvent event) {
        if (!isRegistered(event.getGuild())) {
            event.getTextChannel().sendMessage("체크인/체크아웃 알림이 설정되어있지 않습니다.").queue();
            return;
        }

        Date nextDate = (checkIn.getTime() > checkOut.getTime()) ? checkOut : checkIn;
        event.getTextChannel().sendMessage(nextDate.toString()).queue();
    }

    private void stopChloeBot(MessageReceivedEvent event) {
        disableGuild(event.getGuild());
        event.getTextChannel().sendMessage("체크인/체크아웃 알림을 종료합니다.").queue();
        jda.getPresence().setActivity(Activity.of(DEFAULT, "휴식"));
    }

    private void disableGuild(Guild guild) {
        guilds.remove(guild.getName());
    }

    private void sendAlreadyRunMessage(@NotNull MessageReceivedEvent event) {
        event.getTextChannel().sendMessage("이미 체크인/체크아웃 알림이 등록되었습니다.").queue();
    }

    private void checkStatus(@NotNull MessageReceivedEvent event) {
        event.getTextChannel().sendMessage("정상 작동중입니다. 봇의 주인 : " + System.getProperty("user.name")).queue();
    }

    private void runChloeBot(@NotNull MessageReceivedEvent event) {

        setTimeZone();
        Guild guild = event.getGuild();

        registerGuild(guild);

        TextChannel checkInTextChannel = guild.getTextChannelsByName("✅체크인-체크아웃", true).get(0);
//        TextChannel targetTextChannel = event.getTextChannel();

//        targetTextChannel.sendMessage("체크인/체크아웃 알림을 시작합니다.").queue();
        jda.getPresence().setActivity(Activity.of(DEFAULT, "체크인/체크아웃 알림"));

        Calendar instance = Calendar.getInstance();
        logger.info("Time zone: {}", instance.getTimeZone());

        TimerTask checkInTask = createTask(checkInTextChannel, CHECK_IN_MODE);
        TimerTask checkOutTask = createTask(checkInTextChannel, CHECK_OUT_MODE);

        LocalDateTime runtime = LocalDateTime.now();

        String checkInTime = runtime.toString().substring(0, 10) + " 10:00:00";
        String checkOutTime = runtime.toString().substring(0, 10) + " 18:00:00";

        try {
            checkIn = calculateInitialDate(checkInTime);
            checkOut = calculateInitialDate(checkOutTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(checkInTask, checkIn, ONE_DAY_IN_MILLISECONDS);
        timer.scheduleAtFixedRate(checkOutTask, checkOut, ONE_DAY_IN_MILLISECONDS);
    }

    private Date calculateInitialDate(String time) throws ParseException {
        setTimeZone();
        Date date = dateFormatter.parse(time);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        if (date.getTime() < new Date().getTime()) {
            cal.add(Calendar.DATE, 1);
        }

        logger.info("time: {}", cal.getTime());

        return cal.getTime();
    }

    private static void registerGuild(Guild guild) {
        guilds.put(guild.getName(), guild.getName());
        guilds.keySet().forEach(key -> logger.info("guilds : {}", key));
    }

    private static void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    @NotNull
    private TimerTask createTask(TextChannel textChannel, String mode) {
        return new TimerTask() {
            public void run() {
                Calendar calendar = Calendar.getInstance();

                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

                if (isWeekend(dayOfWeek)) {
                    logger.info("주말은 쉬어야죠, {}", dayOfWeek);
                    return;
                }

                LocalDateTime now = LocalDateTime.now();

                String dateString = now.toString();
                String month = dateString.substring(5, 7);
                String day = dateString.substring(8, 10);
                textChannel.sendMessage(month + "월 " + day + "일 " + mode +
                        "시간입니다~ 스레드에 "+ mode + " 댓글을 남겨주세요!").queue();

                if (mode.equals(CHECK_IN_MODE)) {
                    calendar.setTime(checkIn);
                    calendar.add(Calendar.DATE, 1);
                    checkIn = calendar.getTime();
                    return;
                }

                calendar.setTime(checkOut);
                calendar.add(Calendar.DATE, 1);
                checkOut = calendar.getTime();
            }
        };
    }

    private boolean isWeekend(int dayOfWeek) {
        return dayOfWeek == 1 || dayOfWeek == 7;
    }

    private boolean isValidRequest(@NotNull MessageReceivedEvent event) {
        return isNotBotsMessage(event);
    }

    private boolean isRegistered(Guild guild) {
        return guilds.containsKey(guild.getName());
    }

    private boolean isNotBotsMessage(@NotNull MessageReceivedEvent event) {
        return !event.getAuthor().getName().equals(BOT_NAME);
    }

}
