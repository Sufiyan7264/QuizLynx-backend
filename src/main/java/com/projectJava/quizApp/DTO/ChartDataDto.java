package com.projectJava.quizApp.DTO;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChartDataDto {
    private List<String> labels;
    private List<DatasetDto> datasets;

    @Data
    @Builder
    public static class DatasetDto {
        private String label;
        private List<Number> data; // Can hold Integer or Double
        // Optional: Colors if you want to control from backend
    }
}