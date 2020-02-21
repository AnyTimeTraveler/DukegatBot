package org.simonscode.dukegatbot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;


public class DukegatBot extends TelegramLongPollingBot {

    private static boolean manualOverride = false;

    public static void main(String[] args) {
        // Init bot stuff
        ApiContextInitializer.init();
        TelegramBotsApi api = new TelegramBotsApi();
        DukegatBot bot = new DukegatBot();
        try {
            api.registerBot(bot);
        } catch (TelegramApiRequestException e) {
            System.err.println("Could not register bot!!");
            e.printStackTrace();
        }

        // init Timer
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!manualOverride)
                    bot.checkStatusAndUpdate();
            }
        }, 1000, Config.getInstance().refreshIntervalInMS);

    }

    /**
     * Gets the status of the Teestube
     *
     * @return current status
     */
    private static TSStatus getTSStatus() {
        TSStatus status;
        try {
            URL api = new URL(Config.getInstance().apiEndpoint);
            URLConnection conn = api.openConnection();
            conn.connect();
            byte[] bytes = conn.getInputStream().readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            if (json.contains("\"heartbeat\":true")) {
                status = TSStatus.OPEN;
            } else {
                status = TSStatus.CLOSED;
            }
        } catch (IOException e) {
            status = TSStatus.ERROR;
        }
        return status;
    }


    /**
     * Checks status and sends an update to Telegram if it has changed
     */
    private void checkStatusAndUpdate() {
        TSStatus status = getTSStatus();
        if (!status.equals(Config.getInstance().tsStatus)) {
            Config.getInstance().tsStatus = status;
            Config.getInstance().save();

            notifyTelegram(status);
        }
    }

    /**
     * Announces the new status on Telegram
     *
     * @param status changed status
     */
    private void notifyTelegram(TSStatus status) {
        SendMessage message = new SendMessage().setChatId(Config.getInstance().channelId);
        switch (status) {
            case OPEN:
                message.setText(Config.getInstance().openText);
                break;
            case CLOSED:
                message.setText(Config.getInstance().closeText);
                break;
            case ERROR:
                message.setText(Config.getInstance().errorText);
                break;
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message!");
            e.printStackTrace();
        }
    }

    /**
     * Events from Telegram arrive here
     */
    @Override
    public void onUpdateReceived(Update update) {
        Optional<Message> message = Optional.of(update)
                .map(Update::getMessage);
        Optional<Message> messageFromAdmin = message
                .filter(m -> Config.getInstance().adminIds.contains(m.getFrom().getId()));

        // Command to set target channel
        messageFromAdmin.filter(m -> m.getText().equals("/here"))
                .ifPresent(m -> {
                    Config.getInstance().channelId = String.valueOf(m.getChatId());
                    Config.getInstance().save();
                    reply(m, "Set!");
                });

        // Command to send manual update
        messageFromAdmin.filter(m -> m.getText().equals("/update"))
                .ifPresent(m -> checkStatusAndUpdate());

        // Get user id
        message.filter(m -> m.getText().equals("/id"))
                .ifPresent(m -> {
                    reply(m, "ID:" + m.getFrom().getId());
                });

        // Command to send manual status
        messageFromAdmin.filter(m -> m.getText().startsWith("/status"))
                .ifPresent(m -> {
                    String[] parts = m.getText().split(" ");
                    switch (parts[1]) {
                        case "open":
                        case "offen":
                            Config.getInstance().tsStatus = TSStatus.OPEN;
                            Config.getInstance().save();
                            manualOverride = true;
                            notifyTelegram(TSStatus.OPEN);
                            break;
                        case "closed":
                        case "geschlossen":
                            Config.getInstance().tsStatus = TSStatus.CLOSED;
                            Config.getInstance().save();
                            manualOverride = true;
                            notifyTelegram(TSStatus.CLOSED);
                            break;
                        case "auto":
                            manualOverride = false;
                            break;
                        default:
                            reply(m, "Invalid selection!\n" +
                                    "Try closed or open or auto!");

                    }
                });

        // Command to send manual update
        messageFromAdmin.filter(m -> m.getText().equals("/ping"))
                .ifPresent(m -> reply(m, "Pong!"));


    }

    private void reply(Message m, String text) {
        try {
            execute(new SendMessage(m.getChatId(), text).setReplyToMessageId(m.getMessageId()));
        } catch (TelegramApiException e) {
            System.err.println("Error sending reply!");
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "DukegatBot";
    }

    @Override
    public String getBotToken() {
        return Config.getInstance().botToken;
    }
}
