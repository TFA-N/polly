/*
 * Copyright 2013 Simon Taddiken
 * 
 * This file is part of Polly HTTP API.
 * 
 * Polly HTTP API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * Polly HTTP API is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Polly HTTP API. If not, see http://www.gnu.org/licenses/.
 */
package de.skuzzle.polly.http.api.handler;

import java.io.FileNotFoundException;

import de.skuzzle.polly.http.api.HttpEvent;
import de.skuzzle.polly.http.api.HttpException;
import de.skuzzle.polly.http.api.ResolvedFile;
import de.skuzzle.polly.http.api.answers.HttpAnswer;
import de.skuzzle.polly.http.api.answers.HttpFileAnswer;


public class SingleFileEventHandler extends AbstractFileEventHandler {

    private final ResolvedFile file;
    
    
    
    public SingleFileEventHandler(ResolvedFile file) {
        super(200, false);
        this.file = file;
    }
    

    
    @Override
    protected HttpAnswer handleHttpEvent(String registered, HttpEvent e) 
            throws FileNotFoundException, HttpException {
        
        return new HttpFileAnswer(this.responseCode, this.file);
    }
}
