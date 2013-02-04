/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axiom.c14n.impl;

import org.apache.axiom.c14n.Canonicalizer;

/**
 * @author Christian Geuer-Pollmann <geuerp@apache.org>
 *
 * modified to work with Axiom wrapper by Saliya Ekanayake (esaliya@gmail.com)
 */
public class Canonicalizer20010315OmitComments extends Canonicalizer20010315 {
    /**
     * Constructor Canonicalizer20010315OmitComments
     */
    public Canonicalizer20010315OmitComments() {
        super(false);
    }

    /**
     * @inheritDoc
     */
    public final String engineGetURI() {
        return Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS;
    }

    /**
     * @inheritDoc
     */
    public final boolean engineGetIncludeComments() {
        return false;
    }
}
