package com.javarush.telegram;

public class UserInfo {
    public String myGender; // М или Ж
    public String lookingForGender; // М или Ж
    public String targetGender; // Пол того, кому пишем
    public String openerAuthorGender; // male/female
    public String name; //Имя
    public String sex; //Пол
    public String age; //Возраст
    public String city; //Город
    public String occupation; //Профессия
    public String hobby; //Хобби
    public String handsome; //Красота, привлекательность
    public String wealth; //Доход, богатство
    public String annoys; //Меня раздражает в людях
    public String goals; //Цели знакомства

    private String fieldToString(String str, String description) {
        if (str != null && !str.isEmpty())
            return description + ": " + str + "\n";
        else
            return "";
    }

    @Override
    public String toString() {
        String result = "";

        result += fieldToString(name, "Имя");

        if (targetGender != null && !targetGender.isEmpty()) {
            String genderRu = targetGender.equals("male") ? "Мужчина" : "Женщина";
            result += fieldToString(genderRu, "Пол");
        } else {
            result += fieldToString(sex, "Пол");
        }

        result += fieldToString(age, "Возраст");
        result += fieldToString(city, "Город");
        result += fieldToString(occupation, "Профессия");
        result += fieldToString(hobby, "Хобби");
        result += fieldToString(handsome, "Красота, привлекательность в баллах (максимум 10 баллов)");
        result += fieldToString(wealth, "Доход, богатство");
        result += fieldToString(annoys, "В людях раздражает");
        result += fieldToString(goals, "Цели знакомства");

        return result;
    }

    // Новый метод
    public String getDescriptionWithGender() {
            String genderText = "";
            if ("male".equals(myGender)) {
                genderText = "Я мужчина";
            } else if ("female".equals(myGender)) {
                genderText = "Я женщина";
            } else {
                genderText = "Человек";
            }

            String lookingText = "";
            if ("male".equals(lookingForGender)) {
            lookingText = "ищу мужчину";
        } else if ("female".equals(lookingForGender)) {
            lookingText = "ищу женщину";
        } else if ("any".equals(lookingForGender)) {
            lookingText = "ищу человека (пол не важен)";
        } else {
            lookingText = "цель не указана";
        }

        String details = "";
        if (age != null && !age.isEmpty()) details += " Мне " + age + " лет.";
        if (occupation != null && !occupation.isEmpty()) details += " Работаю: " + occupation + ".";
        if (hobby != null && !hobby.isEmpty()) details += " Хобби: " + hobby + ".";
        if (annoys != null && !annoys.isEmpty()) details += " Раздражает: " + annoys + ".";
        if (goals != null && !goals.isEmpty()) details += " Цель: " + goals + ".";
        if (city != null && !city.isEmpty()) details += " Город: " + city + ".";
        // можно добавить handsome, wealth по желанию

        return genderText + ", " + lookingText + "." + details;
    }

}
