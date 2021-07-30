package org.signum;

import org.apache.commons.lang3.RandomStringUtils;
import picocli.CommandLine;
import signumj.crypto.SignumCrypto;
import signumj.entity.SignumAddress;

import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(
        name = "signum-vag",
        mixinStandardHelpOptions = true,
        version = "signum-vag 1.0",
        description = "Creates a vanity address for the Signum blockchain platform"
)
class VanityAddressGenerator implements Callable<String[]> {

    private final char[] SecretChars = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjklmnpqrstuvwxyz".toCharArray();

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = {"-t", "--target"}, required = true, description = "The targeted vanity part")
    private String target = "";

    @CommandLine.Option(names = {"-p", "--position"}, description = "The position for the vanity part from 1 to 4", defaultValue = "1")
    private Integer position = 1;


    private String getRandomSecret() {
        return RandomStringUtils.random(80, 0, this.SecretChars.length, true, true, this.SecretChars, new SecureRandom());
    }

    private String getAddress(String secret) {
        SignumAddress address = SignumCrypto.getInstance().getAddressFromPassphrase(secret);
        return address.toString();
    }

    private Pattern getMatchPattern() throws Exception {
        switch (position) {
            case 1:
                return Pattern.compile("^S-" + target.toUpperCase());
            case 2:
                return Pattern.compile("^S-.{4}-" + target.toUpperCase());
            case 3:
                return Pattern.compile("^S-(.{4}-){2}" + target.toUpperCase());
            case 4:
                return Pattern.compile("^S-(.{4}-){3}" + target.toUpperCase());
            default:
                throw new Exception("Invalid position");
        }
    }

    private String[] findSecret() throws Exception {
        String generated = "", secret = "";


        Pattern pattern = getMatchPattern();
        Matcher matcher = pattern.matcher(generated);
        while (!matcher.find()) {
            secret = getRandomSecret();
            generated = getAddress(secret);
            matcher = pattern.matcher(generated);
        }

        return new String[]{secret, generated};
    }

    private void validate() {
        if(position < 1 || position > 4){
            throw new CommandLine.ParameterException(spec.commandLine(), "Option --position must be at minimum 1 and maximum 4");
        }

        if (target.length() > 5 && position == 4) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Option --target must not be larger as 5 for last position");
        }

        if (target.length() > 4) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Option --target must not be larger as 4");
        }

        Pattern pattern = Pattern.compile("[1I0O]");
        if(pattern.matcher(target.toUpperCase()).find()){
            throw new CommandLine.ParameterException(spec.commandLine(), "Option --target must not contain 1,I,0 or O");
        }

    }

    @Override
    public String[] call() throws Exception {
        validate();

        System.out.println("Starting search for vanity address. This might take a few moments...");
        String[] result = findSecret();
        System.out.println("Found this address: " + result[1]);
        System.out.println("This is the secret: " + result[0]);

        return result;
    }

    public static void main(String... args) {
        CommandLine commandLine = new CommandLine(new VanityAddressGenerator());
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

}
