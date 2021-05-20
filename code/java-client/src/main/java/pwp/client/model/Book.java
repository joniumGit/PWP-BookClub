package pwp.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import pwp.client.utils.ID;


@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"handle"})
@NoArgsConstructor
public class Book {
    @ID
    @NotNull
    private String handle;
    @NotNull
    @JsonProperty("full_name")
    private String name;
    private String description;
    private Integer pages;
}
