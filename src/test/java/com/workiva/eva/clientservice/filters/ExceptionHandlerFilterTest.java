// Copyright 2018-2019 Workiva Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.workiva.eva.clientservice.filters;

import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;
import com.google.gson.JsonSyntaxException;
import eva.Util;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.workiva.eva.clientservice.exceptions.security.AuthException;

import javax.servlet.FilterChain;

import static org.mockito.Mockito.mock;

public class ExceptionHandlerFilterTest {

  @Test
  public void testExpiredJwtException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new ExpiredJwtException(null, null, "test"))
        .when(chain)
        .doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertEquals("test", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
  }

  @Test
  public void testSignatureException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new SignatureException("test")).when(chain).doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertEquals("test", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
  }

  @Test
  public void testIllegalArgumentException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new IllegalArgumentException("Test: Illegal base64 encoding"))
        .when(chain)
        .doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertEquals(
        "Malformed JWT, not valid Base64 Encoding", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
  }

  @Test
  public void testIllegalArgumentExceptionUnrelatedToBase64() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new IllegalArgumentException("Test: Illegal encoding"))
        .when(chain)
        .doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertNotEquals(
        "Malformed JWT, not valid Base64 Encoding", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
  }

  @Test
  public void testJsonSyntaxException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new JsonSyntaxException("test")).when(chain).doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertEquals("Malformed JWT, not valid JSON", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
  }

  @Test
  public void testAuthException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new AuthException("test")).when(chain).doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertEquals("test", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
  }

  @Test
  public void testGenericException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    Mockito.doThrow(new NullPointerException("Thrown Exception"))
        .when(chain)
        .doFilter(request, response);
    filter.doFilter(request, response, chain);

    String content = response.getContentAsString();
    PersistentArrayMap ednContent = (PersistentArrayMap) Util.read(content);

    Assert.assertEquals("Thrown Exception", ednContent.get(Keyword.intern("message")));
    Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
  }

  @Test
  public void testNoException() throws Exception {
    ExceptionHandlerFilter filter = new ExceptionHandlerFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);

    Assert.assertEquals(HttpStatus.OK.value(), response.getStatus());
  }
}
