import java.util.List;

public record Branch(String name, List<Commit> commits) {
}
