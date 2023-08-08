package Business.Models;

import java.util.List;

public record Tag(String name, List<Commit> commits) {
}
