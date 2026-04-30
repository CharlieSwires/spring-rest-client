package com.charlie.springasyncwebclient.dto;


public class AnswerResponse {

    private String answer;
    private double score;

    public AnswerResponse() {
    }

    public AnswerResponse(String answer, double score) {
        this.answer = answer;
        this.score = score;
    }

    public String getAnswer() {
        return answer;
    }

    public double getScore() {
        return score;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "AnswerResponse{" +
                "answer='" + answer + '\'' +
                ", score=" + score +
                '}';
    }
}