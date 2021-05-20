package pwp.client.http;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import dev.jonium.mason.MasonError;
import dev.jonium.mason.impl.SimpleMason;
import dev.jonium.mason.serialization.Tokens;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import pwp.client.Main;
import pwp.client.utils.Triple;
import pwp.client.utils.Tuple;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Client {

    private final HttpClient client;
    @Getter
    private final String base;
    @Setter
    @Getter
    private String user;

    private static final BlockingQueue<ObjectMapper> mappers = new LinkedBlockingDeque<>(10);

    private static final AtomicReference<Client> instance = new AtomicReference<>();

    static {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                var mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mappers.add(
                        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
                              .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
                              .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                );
            }
            Main.getLogger().fine("Client mappers initialized");
        }).exceptionally(Main::handleException);
    }

    public static <T> Optional<T> fromMapper(FromMapper<T> from) {
        try {
            var mapper = mappers.take();
            try {
                return Optional.ofNullable(from.get(mapper));
            } finally {
                mappers.add(mapper);
            }
        } catch (InterruptedException ignore) {
        }
        return Optional.empty();
    }

    public static <T> Optional<T> read(HttpResponse<String> response, ReadFunction<T> type)
    throws CompletionException
    {
        try {
            var mapper = mappers.take();
            try {
                var body = response.body();
                if (body == null) {
                    return Optional.empty();
                }
                T object = type.read(mapper, body);
                Main.getLogger().fine(object.getClass().getCanonicalName());
                Main.getLogger().fine(object.toString());
                return Optional.of(object);
            } catch (JsonParseException | JsonMappingException e) {
                Main.handleException(e);
                Main.getLogger().warning(
                        "Invalid response "
                        + response.statusCode()
                        + "\n"
                        + response.body()
                );
                return Optional.empty();
            } catch (IOException e) {
                throw new CompletionException(e);
            } finally {
                mappers.add(mapper);
            }
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    public static Client getInstance(List<String> args) {
        var out = instance.get();
        if (Objects.isNull(out)) {
            synchronized (instance) {
                out = instance.get();
                Objects.requireNonNull(args);
                if (Objects.isNull(out)) {
                    try {
                        var addressIndex = args.indexOf("--address");
                        String address = "http://localhost:8000/";
                        if (addressIndex != -1) {
                            address = args.get(addressIndex + 1);
                        }
                        var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
                        out = new Client(client, address);
                        instance.set(out);
                        Main.getLogger().fine("Client created");
                    } catch (Throwable t) {
                        throw new Error("Failure in Application initialization", t);
                    }
                }
            }
        }
        return out;
    }

    public static Client getInstance() {
        return getInstance(null);
    }

    public <T> CompletableFuture<Optional<T>> get(
            String href,
            ReadFunction<T> map
    )
    {
        var b = HttpRequest.newBuilder().GET().header("Accept", Tokens.CONTENT_TYPE);
        Optional.ofNullable(user).ifPresent(u -> b.header("BC-User", u));
        var request = b.uri(URI.create(href)).timeout(Duration.ofSeconds(10)).build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                     .thenApplyAsync(response -> Client.read(response, map), ForkJoinPool.commonPool());
    }

    public <T> CompletableFuture<Optional<T>> get(String href, TypeReference<T> type) {
        return get(href, (om, v) -> om.readValue(v, type));
    }

    public <T> CompletableFuture<Optional<T>> get(String href, JavaType type) {
        return get(href, (om, v) -> om.readValue(v, type));
    }


    public <T> CompletableFuture<Optional<SimpleMason<T>>> getSM(String href, TypeReference<SimpleMason<T>> type) {
        return get(href, (om, v) -> om.readValue(v, type));
    }

    public <T> CompletableFuture<Optional<SimpleMason<T>>> getSM(String href, JavaType type) {
        return get(href, (om, v) -> om.readValue(v, type));
    }

    public <T> CompletableFuture<Triple<Integer, Optional<String>, Optional<MasonError>>> getPostPutBase(
            String href,
            T entity,
            Function<HttpRequest.BodyPublisher, HttpRequest.Builder> builderProvider
    )
    {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var mapper = mappers.take();
                try {
                    return mapper.writeValueAsBytes(entity);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            } catch (InterruptedException ex) {
                throw new CompletionException(ex);
            }
        }, ForkJoinPool.commonPool()).thenCompose(bytes -> {
            var b = builderProvider.apply(HttpRequest.BodyPublishers.ofByteArray(bytes));
            Optional.ofNullable(user).ifPresent(u -> b.header("BC-User", u));
            var request = b.uri(URI.create(href)).timeout(Duration.ofSeconds(10)).build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        }).thenApply(response -> {
                         var status = response.statusCode();
                         if (status >= 400) {
                             return Triple.of(
                                     status,
                                     Optional.empty(),
                                     Client.read(response, (om, v) -> om.readValue(v, new TypeReference<>() {}))
                             );
                         } else {
                             return Triple.of(
                                     status,
                                     response.headers().firstValue("Location"),
                                     Optional.empty()
                             );
                         }
                     }
        );
    }

    public <T> CompletableFuture<Triple<Integer, Optional<String>, Optional<MasonError>>> post(String href, T entity) {
        return getPostPutBase(href, entity, HttpRequest.newBuilder()::POST);
    }

    public <T> CompletableFuture<Triple<Integer, Optional<String>, Optional<MasonError>>> put(String href, T entity) {
        return getPostPutBase(href, entity, HttpRequest.newBuilder()::PUT);
    }

    public <T> CompletableFuture<Tuple<Integer, Optional<MasonError>>> delete(String href) {
        return client.sendAsync(
                HttpRequest.newBuilder().DELETE().uri(URI.create(href)).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            var status = response.statusCode();
            if (status >= 400) {
                return Tuple.of(
                        status,
                        Client.read(response, (om, v) -> om.readValue(v, new TypeReference<>() {}))
                );
            } else {
                return Tuple.of(
                        status,
                        Optional.empty()
                );
            }
        });
    }

    @FunctionalInterface
    public interface ReadFunction<T> {
        T read(ObjectMapper from, String value) throws JsonMappingException, JsonParseException, IOException;
    }

    @FunctionalInterface
    public interface FromMapper<T> {
        T get(ObjectMapper mapper);
    }

}
