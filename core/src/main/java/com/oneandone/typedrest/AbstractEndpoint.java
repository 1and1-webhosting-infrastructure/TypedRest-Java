package com.oneandone.typedrest;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.net.*;
import java.util.*;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import lombok.*;
import org.apache.http.*;
import static org.apache.http.HttpHeaders.*;
import org.apache.http.client.fluent.*;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.*;

/**
 * Base class for building REST endpoints, i.e. remote HTTP resources.
 */
public abstract class AbstractEndpoint
        implements Endpoint {

    @Getter
    protected final URI uri;

    @Getter
    protected final Executor rest;

    /**
     * A set of default HTTP headers to be added to each request.
     */
    protected final Collection<Header> defaultHeaders = new LinkedList<>();

    protected final ObjectMapper json = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .findAndRegisterModules();

    /**
     * Creates a new REST endpoint with an absolute URI.
     *
     * @param rest The REST executor used to communicate with the remote
     * element.
     * @param uri The HTTP URI of the remote element.
     */
    protected AbstractEndpoint(Executor rest, URI uri) {
        this.rest = rest;
        this.uri = uri;

        defaultHeaders.add(new BasicHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
    }

    /**
     * Creates a new REST endpoint with a relative URI.
     *
     * @param parent The parent endpoint containing this one.
     * @param relativeUri The URI of this endpoint relative to the
     * <code>parent</code>'s.
     */
    protected AbstractEndpoint(Endpoint parent, URI relativeUri) {
        this(parent.getRest(), parent.getUri().resolve(relativeUri));

        if (parent instanceof AbstractEndpoint) {
            defaultHeaders.addAll(((AbstractEndpoint) parent).defaultHeaders);
        }
    }

    /**
     * Creates a new REST endpoint with a relative URI.
     *
     * @param parent The parent endpoint containing this one.
     * @param relativeUri The URI of this endpoint relative to the
     * <code>parent</code>'s.
     */
    protected AbstractEndpoint(Endpoint parent, String relativeUri) {
        this(parent, URI.create(relativeUri));
    }

    /**
     * Executes a REST request and wraps HTTP status codes in appropriate
     * {@link Exception} types.
     *
     * @param request The request to execute.
     * @return The HTTP response to the request.
     *
     * @throws IOException Network communication failed.
     * @throws IllegalArgumentException {@link HttpStatus#SC_BAD_REQUEST}
     * @throws IllegalAccessException {@link HttpStatus#SC_UNAUTHORIZED} or
     * {@link HttpStatus#SC_FORBIDDEN}
     * @throws FileNotFoundException {@link HttpStatus#SC_NOT_FOUND} or
     * {@link HttpStatus#SC_GONE}
     * @throws IllegalStateException
     * {@link HttpStatus#SC_CONFLICT}, {@link HttpStatus#SC_PRECONDITION_FAILED}
     * or {@link HttpStatus#SC_REQUESTED_RANGE_NOT_SATISFIABLE}
     * @throws RuntimeException Other non-success status code.
     */
    protected HttpResponse executeAndHandle(Request request)
            throws IOException, IllegalArgumentException, IllegalAccessException, FileNotFoundException, IllegalStateException {
        HttpResponse response = execute(request);
        handleResponse(response);
        return response;
    }

    /**
     * Executes a REST request adding any configured {@link #defaultHeaders}.
     *
     * @param request The request to execute.
     * @return The HTTP response to the request.
     *
     * @throws IOException Network communication failed.
     * @throws RuntimeException Other non-success status code.
     */
    protected HttpResponse execute(Request request)
            throws IOException {
        defaultHeaders.forEach(request::addHeader);
        return rest.execute(request).returnResponse();
    }

    /**
     * Handles the response of a REST request and wraps HTTP status codes in
     * appropriate {@link Exception} types.
     *
     * @param response The response to handle.
     * 
     * @throws IOException Network communication failed.
     * @throws IllegalArgumentException {@link HttpStatus#SC_BAD_REQUEST}
     * @throws IllegalAccessException {@link HttpStatus#SC_UNAUTHORIZED} or
     * {@link HttpStatus#SC_FORBIDDEN}
     * @throws FileNotFoundException {@link HttpStatus#SC_NOT_FOUND} or
     * {@link HttpStatus#SC_GONE}
     * @throws IllegalStateException
     * {@link HttpStatus#SC_CONFLICT}, {@link HttpStatus#SC_PRECONDITION_FAILED}
     * or {@link HttpStatus#SC_REQUESTED_RANGE_NOT_SATISFIABLE}
     * @throws RuntimeException Other non-success status code.
     */
    protected void handleResponse(HttpResponse response)
            throws IOException, IllegalArgumentException, IllegalAccessException, FileNotFoundException, IllegalStateException {
        handleLinks(response);
        handleAllow(response);
        handleErrors(response);
    }

    /**
     * Wraps HTTP status codes in appropriate {@link Exception} types.
     *
     * @param response The response to check for errors.
     *
     * @throws IOException Network communication failed.
     * @throws IllegalArgumentException {@link HttpStatus#SC_BAD_REQUEST}
     * @throws IllegalAccessException {@link HttpStatus#SC_UNAUTHORIZED} or
     * {@link HttpStatus#SC_FORBIDDEN}
     * @throws FileNotFoundException {@link HttpStatus#SC_NOT_FOUND} or
     * {@link HttpStatus#SC_GONE}
     * @throws IllegalStateException
     * {@link HttpStatus#SC_CONFLICT}, {@link HttpStatus#SC_PRECONDITION_FAILED}
     * or {@link HttpStatus#SC_REQUESTED_RANGE_NOT_SATISFIABLE}
     * @throws RuntimeException Other non-success status code.
     */
    protected void handleErrors(HttpResponse response)
            throws IOException, IllegalArgumentException, IllegalAccessException, FileNotFoundException, IllegalStateException {
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() <= 299) {
            return;
        }

        String message = statusLine.toString();
        HttpEntity entity = response.getEntity();
        String body;
        if (entity == null) {
            body = null;
        } else {
            body = EntityUtils.toString(entity);
            Header contentType = entity.getContentType();
            if ((contentType != null) && contentType.getValue().startsWith("application/json")) {
                JsonNode messageNode = json.readTree(body).get("message");
                if (messageNode != null) {
                    message = messageNode.asText();
                }
            }
        }

        Exception inner = (body == null) ? null : new HttpException(body);
        switch (statusLine.getStatusCode()) {
            case HttpStatus.SC_BAD_REQUEST:
                throw new IllegalArgumentException(message, inner);
            case HttpStatus.SC_UNAUTHORIZED:
            case HttpStatus.SC_FORBIDDEN:
                throw new IllegalAccessException(message);
            case HttpStatus.SC_NOT_FOUND:
            case HttpStatus.SC_GONE:
                throw new FileNotFoundException(message);
            case HttpStatus.SC_CONFLICT:
            case HttpStatus.SC_PRECONDITION_FAILED:
            case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
                throw new IllegalStateException(message, inner);
            default:
                throw new RuntimeException(message, inner);
        }
    }

    /**
     * Handles links embedded in an HTTP response.
     *
     * @param response The response to check for links.
     */
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    private void handleLinks(HttpResponse response) {
        Map<String, Set<URI>> links = new HashMap<>();
        Map<String, String> linkTemplates = new HashMap<>();

        handleHeaderLinks(response, links, linkTemplates);

        this.links = unmodifiableMap(links);
        this.linkTemplates = unmodifiableMap(linkTemplates);
    }

    /**
     * Handles links embedded in HTTP response headers.
     *
     * @param response The response to check for links.
     * @param links A dictionary to add found links to.
     * @param linkTemplates A dictionary to add found link templates to.
     */
    protected void handleHeaderLinks(HttpResponse response, Map<String, Set<URI>> links, Map<String, String> linkTemplates) {
        for (Header header : response.getHeaders("Link")) {
            for (HeaderElement element : header.getElements()) {
                String href = element.getName().substring(1, element.getName().length() - 1);

                NameValuePair relParameter = element.getParameterByName("rel");
                if (relParameter != null) {
                    NameValuePair templatedParameter = element.getParameterByName("templated");
                    if (templatedParameter != null && templatedParameter.getValue().equals("true")) {
                        String rel = relParameter.getValue();
                        linkTemplates.put(rel, href);
                    } else {
                        String rel = relParameter.getValue();
                        Set<URI> linkSet = links.get(rel);
                        if (linkSet == null) {
                            links.put(rel, linkSet = new HashSet<>());
                        }
                        linkSet.add(uri.resolve(href));
                    }
                }
            }
        }
    }

    // NOTE: Always replace entire dictionary rather than modifying it to ensure thread-safety.
    private Map<String, Set<URI>> links = unmodifiableMap(new HashMap<>());

    @Override
    public Set<URI> getLinks(String rel) {
        Set<URI> uris = links.get(rel);
        return (uris == null) ? new HashSet<>() : unmodifiableSet(uris);
    }

    @Override
    public URI link(String rel) {
        Set<URI> linkSet = getLinks(rel);
        if (linkSet.isEmpty()) {
            // Lazy lookup
            try {
                executeAndHandle(Request.Head(uri));
            } catch (IOException | IllegalAccessException | RuntimeException ex) {
                throw new RuntimeException("No link with rel=" + rel + " provided by endpoint " + getUri() + ".", ex);
            }

            linkSet = getLinks(rel);
            if (linkSet.isEmpty()) {
                throw new RuntimeException("No link with rel=" + rel + " provided by endpoint " + getUri() + ".");
            }
        }

        return linkSet.iterator().next();
    }

    // NOTE: Always replace entire dictionary rather than modifying it to ensure thread-safety.
    private Map<String, String> linkTemplates = unmodifiableMap(new HashMap<>());

    @Override
    public UriTemplate linkTemplate(String rel) {
        String template = linkTemplates.get(rel);
        if (template == null) {
            // Lazy lookup
            try {
                executeAndHandle(Request.Head(uri));
            } catch (IOException | IllegalAccessException | RuntimeException ex) {
                // HTTP HEAD server-side implementation is optional
            }

            template = linkTemplates.get(rel);
        }

        // Create new template instance for each request because UriTemplate is not immutable
        return (template == null) ? null : UriTemplate.fromTemplate(template);
    }

    /**
     * Handles allowed HTTP verbs reported by the server.
     *
     * @param response The response to check for the "Allow" header.
     */
    private void handleAllow(HttpResponse response) {
        allowedVerbs = unmodifiableSet(stream(response.getHeaders("Allow"))
                .filter(x -> x.getName().equals("Allow"))
                .flatMap(x -> stream(x.getElements())).map(x -> x.getName())
                .collect(toSet()));
    }

    // NOTE: Always replace entire set rather than modifying it to ensure thread-safety.
    private Set<String> allowedVerbs = unmodifiableSet(new HashSet<>());

    /**
     * Shows whether the server has indicated that a specific HTTP verb is
     * currently allowed.
     *
     * Uses cached data from last response.
     *
     * @param verb The HTTP verb (e.g. GET, POST, ...) to check.
     * @return An indicator whether the verb is allowed. If no request has been
     * sent yet or the server did not specify allowed verbs
     * {@link Optional#empty()} is returned.
     */
    protected Optional<Boolean> isVerbAllowed(String verb) {
        if (allowedVerbs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(allowedVerbs.contains(verb));
    }
}