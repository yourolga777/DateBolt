package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "dating_Javarush_ai_bot";

    private static final Dotenv dotenv = Dotenv.load();
    public static final String TELEGRAM_BOT_TOKEN = dotenv.get("TELEGRAM_BOT_TOKEN");
    public static final String OPEN_AI_TOKEN = dotenv.get("OPEN_AI_TOKEN");

    private ChatGPTService chatGPT;
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();

    // Для /message
    private String messageUserGender;
    private String messagePartnerGender;
    private int messageSetupStep;

    private UserInfo me;
    private UserInfo shi;
    private int questionCount;
    private int profileStep;
    private int openerStep;
    private String openerAuthorGender;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
        this.chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
        this.chatGPT.setSystemPrompt(loadPrompt("main"));
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String message = getMessageText();



        // START
        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            sendTextMessage(loadMessage("main"));
            showMainMenu("Главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        // GPT
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            sendTextMessage(loadMessage("gpt"));
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");
            // Добавляем информацию о поле пользователя, если она известна
            if (me != null && me.myGender != null) {
                String userGenderText = "male".equals(me.myGender) ? "мужчина" : "женщина";
                prompt = prompt + "\n\nПользователь, задающий вопрос: " + userGenderText + ". Обращайся к нему/ней соответственно.";
            }

            Message msg = sendTextMessage("Подождите пару секунд, ChatGPT думает...");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }

        // DATE
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            if (me == null || me.myGender == null || me.myGender.isEmpty()) {
                sendTextButtonsMessage("Для начала скажи, кто ты?",
                        "Мужчина", "date_gender_male",
                        "Женщина", "date_gender_female");
            } else {
                showDateStarsMenu();
            }
            return;
        }
        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if ("date_gender_male".equals(query)) {
                if (me == null) me = new UserInfo();
                me.myGender = "male";
                showDateStarsMenu();
                return;
            }
            if ("date_gender_female".equals(query)) {
                if (me == null) me = new UserInfo();
                me.myGender = "female";
                showDateStarsMenu();
                return;
            }
            if (query != null && query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор! \nТвоя задача - добиться свидания \uD83E\uDD70 за 5 сообщений.");
                String prompt = loadPrompt(query);
                if (me != null && me.myGender != null) {
                    String userGenderText = "male".equals(me.myGender) ? "мужчина" : "женщина";
                    prompt = prompt + "\n\nПользователь, который общается со звездой – " + userGenderText + ". Учитывай это в диалоге.";
                }
                chatGPT.setPrompt(prompt);
                return;
            }
            Message msg = sendTextMessage("Подождите, ваш собеседник набирает текст...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        // MESSAGE
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            list.clear();
            messageSetupStep = 1;
            messageUserGender = null;
            messagePartnerGender = null;
            sendTextButtonsMessage("Сначала скажи, кто ты?",
                    "Мужчина", "msg_user_male",
                    "Женщина", "msg_user_female");
            return;
        }
        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (messageSetupStep == 1) {
                if ("msg_user_male".equals(query)) {
                    messageUserGender = "male";
                } else if ("msg_user_female".equals(query)) {
                    messageUserGender = "female";
                } else {
                    return;
                }
                messageSetupStep = 2;
                sendTextButtonsMessage("А кто твой собеседник?",
                        "Мужчина", "msg_partner_male",
                        "Женщина", "msg_partner_female");
                return;
            }
            if (messageSetupStep == 2) {
                if ("msg_partner_male".equals(query)) {
                    messagePartnerGender = "male";
                } else if ("msg_partner_female".equals(query)) {
                    messagePartnerGender = "female";
                } else {
                    return;
                }
                messageSetupStep = 3;
                sendTextButtonsMessage("Пришлите в чат вашу переписку (каждое сообщение отдельным текстом).",
                        "Следующее сообщение", "message_next",
                        "Пригласить на свидание", "message_date");
                return;
            }
            if (messageSetupStep == 3) {
                if (query != null && query.startsWith("message_")) {
                    String prompt = loadPrompt(query);
                    String userChatHistory = String.join("\n\n", list);
                    String userGenderText = "male".equals(messageUserGender) ? "мужчина" : "женщина";
                    String partnerGenderText = "male".equals(messagePartnerGender) ? "мужчина" : "женщина";
                    String genderInfo = "Пользователь (отправитель сообщений): " + userGenderText +
                            "\nСобеседник: " + partnerGenderText;
                    String fullPrompt = prompt + "\n\n" + genderInfo;
                    Message msg = sendTextMessage("Подождите пару секунд, ChatGPT думает...");
                    String answer = chatGPT.sendMessage(fullPrompt, userChatHistory);
                    updateTextMessage(msg, answer);
                    return;
                }
                if (!isMessageCommand()) {
                    list.add(message);
                    sendTextMessage("Сообщение добавлено. Пришлите следующее или нажмите кнопку.");
                }
                return;
            }
            return;
        }

        // PROFILE
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            me = new UserInfo();
            profileStep = 1;
            sendTextButtonsMessage("Кто ты?",
                    "Мужчина", "male",
                    "Женщина", "female");
            return;
        }
        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (profileStep == 1) {
                if ("male".equals(query)) me.myGender = "male";
                else if ("female".equals(query)) me.myGender = "female";
                else return;
                profileStep = 2;
                sendTextButtonsMessage("Кого ты ищешь?",
                        "Женщину", "looking_female",
                        "Мужчину", "looking_male",
                        "Не важно", "looking_any");
                return;
            }
            if (profileStep == 2) {
                if ("looking_female".equals(query)) me.lookingForGender = "female";
                else if ("looking_male".equals(query)) me.lookingForGender = "male";
                else if ("looking_any".equals(query)) me.lookingForGender = "any";
                else return;
                profileStep = 3;
                sendTextMessage("Сколько тебе лет?");
                return;
            }
            if (profileStep == 3) {
                me.age = message;
                profileStep = 4;
                sendTextMessage("Кем ты работаешь?");
                return;
            }
            if (profileStep == 4) {
                me.occupation = message;
                profileStep = 5;
                sendTextMessage("У тебя есть хобби?");
                return;
            }
            if (profileStep == 5) {
                me.hobby = message;
                profileStep = 6;
                sendTextMessage("Что тебе НЕ нравится в людях?");
                return;
            }
            if (profileStep == 6) {
                me.annoys = message;
                profileStep = 7;
                sendTextMessage("Цель знакомства?");
                return;
            }
            if (profileStep == 7) {
                me.goals = message;
                String aboutMyself = me.getDescriptionWithGender();
                String prompt = loadPrompt("profile");
                Message msg = sendTextMessage("Подожди пару секунд, \uD83E\uDDE0 ChatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, aboutMyself);
                updateTextMessage(msg, answer);
                profileStep = 0;
                return;
            }
            return;
        }

        // OPENER
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            shi = new UserInfo();
            openerStep = 1;
            openerAuthorGender = null;
            sendTextButtonsMessage("Кто ты?",
                    "Мужчина", "author_male",
                    "Женщина", "author_female");
            return;
        }
        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (openerStep == 1) {
                if ("author_male".equals(query)) openerAuthorGender = "male";
                else if ("author_female".equals(query)) openerAuthorGender = "female";
                else return;
                openerStep = 2;
                sendTextButtonsMessage("Кого хочешь пригласить?",
                        "Мужчину", "target_male",
                        "Женщину", "target_female");
                return;
            }
            if (openerStep == 2) {
                if ("target_male".equals(query)) shi.targetGender = "male";
                else if ("target_female".equals(query)) shi.targetGender = "female";
                else return;
                openerStep = 3;
                sendTextMessage("Как зовут этого человека?");
                return;
            }
            if (openerStep == 3) {
                shi.name = message;
                openerStep = 4;
                sendTextMessage("Сколько ему/ей лет?");
                return;
            }
            if (openerStep == 4) {
                shi.age = message;
                openerStep = 5;
                sendTextMessage("Какое у него/нее хобби?");
                return;
            }
            if (openerStep == 5) {
                shi.hobby = message;
                openerStep = 6;
                sendTextMessage("Кем он/она работает?");
                return;
            }
            if (openerStep == 6) {
                shi.occupation = message;
                openerStep = 7;
                sendTextMessage("Какова цель знакомства для него/нее?");
                return;
            }
            if (openerStep == 7) {
                shi.goals = message;
                String targetDescription = shi.toString();
                String prompt = loadPrompt("opener");
                String authorFirstPerson = "male".equals(openerAuthorGender) ? "я (мужчина)" : "я (женщина)";
                String fullPrompt = prompt + "\n\n" +
                        "ВАЖНО: Сообщение должно быть написано от первого лица, от " + authorFirstPerson + ".\n" +
                        "Обращайся к цели с учётом её пола.\n\n" +
                        "Информация о цели:\n" + targetDescription;
                Message msg = sendTextMessage("Подожди пару секунд, \uD83E\uDDE0 ChatGPT думает...");
                String answer = chatGPT.sendMessage(fullPrompt, "");
                updateTextMessage(msg, answer);
                openerStep = 0;
                return;
            }
            return;
        }
    }

    // Вспомогательный метод для меню звёзд
    private void showDateStarsMenu() {
        String text = loadMessage("date");
        sendTextButtonsMessage(text,
                "Ариана Гранде", "date_grande",
                "Марго Робби", "date_robbie",
                "Зендея", "date_zendaya",
                "Райн Гослинг", "date_gosling",
                "Том Харди", "date_hardy");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }

}