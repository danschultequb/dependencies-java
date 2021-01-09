package qub;

public class QubDependenciesListParameters
{
    private final CharacterWriteStream output;
    private final VerboseCharacterToByteWriteStream verbose;
    private final Folder folder;
    private final EnvironmentVariables environmentVariables;

    public QubDependenciesListParameters(CharacterWriteStream output, VerboseCharacterToByteWriteStream verbose, Folder folder, EnvironmentVariables environmentVariables)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(verbose, "verbose");
        PreCondition.assertNotNull(folder, "folder");
        PreCondition.assertNotNull(environmentVariables, "environmentVariables");

        this.output = output;
        this.verbose = verbose;
        this.folder = folder;
        this.environmentVariables = environmentVariables;
    }

    public CharacterWriteStream getOutput()
    {
        return this.output;
    }

    public VerboseCharacterToByteWriteStream getVerbose()
    {
        return this.verbose;
    }

    public Folder getFolder()
    {
        return this.folder;
    }

    public EnvironmentVariables getEnvironmentVariables()
    {
        return this.environmentVariables;
    }
}