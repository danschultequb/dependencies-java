package qub;

public class QubDependenciesUpdateParameters
{
    private final CharacterWriteStream output;
    private final VerboseCharacterWriteStream verbose;
    private final Folder folder;
    private final EnvironmentVariables environmentVariables;
    private boolean intellij;

    public QubDependenciesUpdateParameters(CharacterWriteStream output, VerboseCharacterWriteStream verbose, Folder folder, EnvironmentVariables environmentVariables)
    {
        PreCondition.assertNotNull(output, "output");
        PreCondition.assertNotNull(verbose, "verbose");
        PreCondition.assertNotNull(folder, "folder");
        PreCondition.assertNotNull(environmentVariables, "environmentVariables");

        this.output = output;
        this.verbose = verbose;
        this.folder = folder;
        this.environmentVariables = environmentVariables;
        this.intellij = false;
    }

    public CharacterWriteStream getOutput()
    {
        return this.output;
    }

    public VerboseCharacterWriteStream getVerbose()
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

    public boolean getIntellij()
    {
        return this.intellij;
    }

    public QubDependenciesUpdateParameters setIntellij(boolean intellij)
    {
        this.intellij = intellij;
        return this;
    }
}