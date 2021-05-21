package pwp.client.http;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.NoSuchElementException;
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

    private static final BlockingDeque<ObjectMapper> mappers = new LinkedBlockingDeque<>(10);

    private static final AtomicReference<Client> instance = new AtomicReference<>();

    static {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                var mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mappers.add(mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
            }
            Main.getLogger().fine("Client mappers initialized");
        }).exceptionally(Main::handleException);
    }

    public static <T> Optional<T> useMapper(FromMapper<T> from) {
        try {
            var mapper = mappers.takeFirst();
            var config = mapper.getDeserializationConfig();
            var config2 = mapper.getSerializationConfig();
            try {
                return Optional.ofNullable(from.get(mapper));
            } catch (IOException e) {
                Main.handleException(e);
            } finally {
                mapper.setConfig(config);
                mapper.setConfig(config2);
                mappers.addLast(mapper);
            }
        } catch (InterruptedException ignore) {
        }
        return Optional.empty();
    }

    public static <T> Optional<T> read(HttpResponse<String> response, ReadFunction<T> type)
    throws CompletionException
    {
        try {
            return useMapper(om -> {
                try {
                    var body = response.body();
                    if (body == null) {
                        return null;
                    }
                    T object = type.read(om, body);
                    Main.getLogger().fine(object.getClass().getCanonicalName());
                    Main.getLogger().fine(object.toString());
                    return object;
                } catch (JsonParseException | JsonMappingException e) {
                    Main.handleException(e);
                    Main.getLogger().warning(
                            "Invalid response "
                            + response.statusCode()
                            + "\n"
                            + response.body()
                    );
                    return null;
                }
            });
        } catch (Throwable t) {
            Main.handleException(t);
            throw new CompletionException(t);
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
        var uri = URI.create(href);
        Main.getLogger().finest(uri.toString());
        var request = b.uri(uri).timeout(Duration.ofSeconds(10)).build();
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
        return CompletableFuture.supplyAsync(
                () -> useMapper(om -> om.writeValueAsBytes(entity)),
                ForkJoinPool.commonPool()
        ).thenCompose(bytes -> {
            var b =
                    builderProvider.apply(HttpRequest.BodyPublishers.ofByteArray(
                            bytes.orElseThrow(() -> new CompletionException(new NoSuchElementException()))
                    ));
            Optional.ofNullable(user).ifPresent(u -> b.header("BC-User", u));
            var uri = URI.create(href);
            Main.getLogger().finest(uri.toString());
            var request = b.uri(uri).timeout(Duration.ofSeconds(10)).build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        }).thenApply(response -> {
                         var status = response.statusCode();
                         if (status >= 400) {
                             Main.getLogger().fine(response.body());
                             return Triple.of(
                                     status,
                                     Optional.empty(),
                                     Client.read(
                                             response,
                                             (om, v) -> om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
                                                          .readValue(v, MasonError.class)
                                     )
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
        var uri = URI.create(href);
        Main.getLogger().finest(uri.toString());
        return client.sendAsync(
                HttpRequest.newBuilder().DELETE().uri(uri).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            var status = response.statusCode();
            if (status >= 400) {
                Main.getLogger().fine(response.body());
                return Tuple.of(
                        status,
                        Client.read(
                                response,
                                (om, v) -> om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
                                             .readValue(v, MasonError.class)
                        )
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
        T read(ObjectMapper from, String value) throws JsonMappingException,
                                                       JsonParseException,
                                                       IOException;
    }

    @FunctionalInterface
    public interface FromMapper<T> {
        T get(ObjectMapper mapper) throws IOException;
    }

}
