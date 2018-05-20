/*
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.mobile.api.id6o8xnn7d78;

import java.util.*;

import com.amazonaws.mobile.api.id6o8xnn7d78.model.Pets;
import com.amazonaws.mobile.api.id6o8xnn7d78.model.NewPetResponse;
import com.amazonaws.mobile.api.id6o8xnn7d78.model.NewPet;
import com.amazonaws.mobile.api.id6o8xnn7d78.model.Empty;
import com.amazonaws.mobile.api.id6o8xnn7d78.model.Pet;


@com.amazonaws.mobileconnectors.apigateway.annotation.Service(endpoint = "https://6o8xnn7d78.execute-api.ap-south-1.amazonaws.com/test")
public interface PetStoreClient {


    /**
     * A generic invoker to invoke any API Gateway endpoint.
     * @param request
     * @return ApiResponse
     */
    com.amazonaws.mobileconnectors.apigateway.ApiResponse execute(com.amazonaws.mobileconnectors.apigateway.ApiRequest request);
    
    /**
     * 
     * 
     * @return void
     */
    @com.amazonaws.mobileconnectors.apigateway.annotation.Operation(path = "/", method = "GET")
    void rootGet();
    
    /**
     * 
     * 
     * @param type 
     * @param page 
     * @return Pets
     */
    @com.amazonaws.mobileconnectors.apigateway.annotation.Operation(path = "/pets", method = "GET")
    Pets petsGet(
            @com.amazonaws.mobileconnectors.apigateway.annotation.Parameter(name = "type", location = "query")
            String type,
            @com.amazonaws.mobileconnectors.apigateway.annotation.Parameter(name = "page", location = "query")
            String page);
    
    /**
     * 
     * 
     * @param body 
     * @return NewPetResponse
     */
    @com.amazonaws.mobileconnectors.apigateway.annotation.Operation(path = "/pets", method = "POST")
    NewPetResponse petsPost(
            NewPet body);
    
    /**
     * 
     * 
     * @return Empty
     */
    @com.amazonaws.mobileconnectors.apigateway.annotation.Operation(path = "/pets", method = "OPTIONS")
    Empty petsOptions();
    
    /**
     * 
     * 
     * @param petId 
     * @return Pet
     */
    @com.amazonaws.mobileconnectors.apigateway.annotation.Operation(path = "/pets/{petId}", method = "GET")
    Pet petsPetIdGet(
            @com.amazonaws.mobileconnectors.apigateway.annotation.Parameter(name = "petId", location = "path")
            String petId);
    
    /**
     * 
     * 
     * @param petId 
     * @return Empty
     */
    @com.amazonaws.mobileconnectors.apigateway.annotation.Operation(path = "/pets/{petId}", method = "OPTIONS")
    Empty petsPetIdOptions(
            @com.amazonaws.mobileconnectors.apigateway.annotation.Parameter(name = "petId", location = "path")
            String petId);
    
}

