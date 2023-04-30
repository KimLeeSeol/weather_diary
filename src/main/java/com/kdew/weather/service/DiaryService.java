package com.kdew.weather.service;


import com.kdew.weather.WeatherApplication;
import com.kdew.weather.domain.DateWeather;
import com.kdew.weather.domain.Diary;
import com.kdew.weather.error.InvalidDate;
import com.kdew.weather.repository.DateWeatherRepository;
import com.kdew.weather.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;


    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;
    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class); // 로거 생성

    // DB에 저장
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void saveWeatherDate() {
        // 매일 새벽에 날씨 데이터를 가져와서 1시에 저장
        dateWeatherRepository.save(getWeatherFromApi());
        logger.info("오늘도 날씨 데이터 잘 가져옴");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.info("started to create diary");

        // DB에 저장되어있는 날씨 데이터 가져오기
        DateWeather dateWeather = getDateWeather(date);

        Diary nowDiary = new Diary(); // db에 저장
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);

        diaryRepository.save(nowDiary);
        logger.info("end to create diary");
    }

    // DB에 저장할 과거 날씨 데이터
    private DateWeather getWeatherFromApi() {
        String weatherData = getWeatherString(); // 날씨 데이터 가져오기
        Map<String, Object> parseWeather = parseWeather(weatherData); // json 파싱

        // 파싱된 날씨 데이터를 entity에 넣어주기
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parseWeather.get("main").toString());
        dateWeather.setIcon(parseWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parseWeather.get("temp"));

        return dateWeather;
    }

    private DateWeather getDateWeather(LocalDate date) {
        // 사용자가 원하는 날짜가 DB에 존재하는지 확인
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
        if (dateWeatherListFromDB.size() == 0) {
            // 만약 날씨 정보가 없으면!
            // 정책상 유료니까.. 현재 날씨를 가져오도록 함..!!
            return getWeatherFromApi();
        }

        else {
            return dateWeatherListFromDB.get(0); // 만약 날씨 정보가 있으면 첫번째꺼 가져오기
        }
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
        /*if(date.isAfter(LocalDate.ofYearDay(3050, 1))) {
            throw new InvalidDate();
        }*/

        return diaryRepository.findAllByDate(date); // 일기를 날짜에 따라 조회
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findAllByDateBetween(startDate,endDate);
    }

    // 찾는 날짜에 일기가 여러개이면 첫번째 내용만 수정
    public void updateDiary(LocalDate date, String text) {
        Diary nowDiary = diaryRepository.getFirstByDate(date); // 파라미터로 들어온 날짜로 get
        nowDiary.setText(text); // 파라미터로 들어온 내용으로 set

        diaryRepository.save(nowDiary); // db에 저장
    }

    public void deleteDiary(LocalDate date) {
        diaryRepository.deleteAllByDate(date);
    }

    private String getWeatherString() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // 요청을 보낼 수 있는 HttpURLConnection 열었음
            connection.setRequestMethod("GET"); // 요청 보냄
            int responseCode = connection.getResponseCode(); //응답코드 저장
            BufferedReader br; // 에러코드나 응답 코드가 길 수 있으니까 성능향상을 위해 BufferedReader 사용
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream())); // 응답 객체 받음
            }
            else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream())); // 오류 메시지 받음

            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine); // 결과값 쌓음
            }

            br.close();
            return response.toString();

        } catch (Exception e) {
            return "failed to get response";
        }
    }

    private Map<String,Object> parseWeather(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString); // jsonObject에 파싱 결과값 담음
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Map<String,Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");
        resultMap.put("temp", mainData.get("temp"));

        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherData = (JSONObject) weatherArray.get(0);
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));

        return resultMap;
    }



}
