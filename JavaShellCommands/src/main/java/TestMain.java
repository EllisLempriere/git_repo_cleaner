import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TestMain {

    public static void main(String[] args) {
//        runSingleCommand("dir");
//        System.out.println();

//        runCommand(new String[]{"C:\\Program Files\\Git\\cmd\\git.exe", "status"});

//        runMultipleCommands(new String[]{"cd \"C:\\Users\\ellis\\Documents\\repos\"", "echo \"Hello World!\" > foo.txt",
//            "more foo.txt", "del foo.txt"});
    }


    public static void runSingleCommand(String command) {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        try {
            pb.redirectErrorStream(true);
            Process process = pb.start();
//            System.out.println("1Process is alive:" + process.isAlive());

            BufferedReader inStreamReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

//            System.out.println("2Process is alive:" + process.isAlive());

            String line = inStreamReader.readLine();
            while (line != null) {
                System.out.println(line);
                line = inStreamReader.readLine();

//                System.out.println("3Process is alive:" + process.isAlive());
            }

//            System.out.println("4Process is alive:" + process.isAlive());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void runMultipleCommands(String[] commands) {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        try {
            pb.redirectErrorStream(true);
            Process process = pb.start();

            OutputStreamWriter inputWriter = new OutputStreamWriter(process.getOutputStream());
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            inputWriter.write("cd C:\\\n");
            inputWriter.write("fasdfasdf\n");
            inputWriter.write("exit\n");
            inputWriter.flush();

            String line = outputReader.readLine();
            while (line != null) {
                System.out.println(line);
                line = outputReader.readLine();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static CommandResult runCommand(String[] commandParts) {
        CommandResult res = new CommandResult();

        List<String> command = new ArrayList<>(List.of(new String[]{"cmd.exe", "/c"}));
        command.addAll(List.of(commandParts));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File("C:\\Users\\ellis\\Documents\\repos\\git_repo_cleaner"));
        try {
            Process process = pb.start();

            BufferedReader cmdOutputReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            BufferedReader cmdErrorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = cmdOutputReader.readLine()) != null)
                res.output.add(line);

            while ((line = cmdErrorReader.readLine()) != null)
                res.errorOutput.add(line);

            res.exitValue = process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return res;
    }

    private static class CommandResult {
        public List<String> output = new ArrayList<>();
        public List<String> errorOutput = new ArrayList<>();
        public int exitValue;
    }
}
