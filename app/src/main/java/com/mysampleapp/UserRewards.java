package com.mysampleapp;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "ecreators_user_app_rewards")
public class UserRewards {
    private String userId;
    private String category;
    private Long rewards;
    @DynamoDBHashKey(attributeName = "user_id")
    public String  getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    @DynamoDBAttribute(attributeName="Category")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    @DynamoDBAttribute(attributeName="Rewards")
    public Long getRewards() {
        return rewards;
    }

    public void setRewards(Long rewards) {
        this.rewards = rewards;
    }
}
