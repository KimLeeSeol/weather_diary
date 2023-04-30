package com.kdew.weather.repository;

import com.kdew.weather.domain.DateWeather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DateWeatherRepository extends JpaRepository<DateWeather, Integer> {
    List<DateWeather> findAllByDate(LocalDate date);
}
