package pwp.client.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class Review {
    @NotNull
    private String user;
    @NotNull
    private String book;
    @NotNull
    private Integer stars;
    @NotNull
    private String title;
    private String content;
}
