/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lttldrgn.portochat.client;

import java.util.regex.Pattern;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Class that overrides PlainDocument to filter text patterns and max length
 * @author Brandon
 */
public class FilteredPlainDoc extends PlainDocument {
        private int maxLength;
        private String acceptedPattern;
        
        public FilteredPlainDoc(String acceptedPattern, int maxLength) {
            this.acceptedPattern = acceptedPattern;
            this.maxLength = maxLength;
        }
        
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            
            if (lengthIsValid(str) && containsValidChars(str))
            {
                super.insertString(offs, str, a);
            }
        }
        
        @Override
        public void replace(int offs, int length, String str, AttributeSet a) throws BadLocationException {
            
            if (containsValidChars(str))
            {
                super.replace(offs, length, str, a);
            }
        }
        
        private boolean containsValidChars(String str) {
            return acceptedPattern == null ? true : Pattern.matches(acceptedPattern, str);
        }
        
        private boolean lengthIsValid(String str) {
            return (getLength() + str.length()) <= maxLength;
        }
    }