package pwp.client.path;

import lombok.*;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Relations {
    ADD("add"),
    BC_BOOKS("bc:books-all", "Books"),
    BC_CLUBS("bc:clubs-all", "Clubs"),
    BC_HOME("bc:home", "Home"),
    BC_USERS("bc:users-all", "Users"),
    COLLECTION("collection"),
    DELETE("delete"),
    EDIT("edit"),
    SELF("self");
    private final String value;
    @Setter
    private String display = null;
}
