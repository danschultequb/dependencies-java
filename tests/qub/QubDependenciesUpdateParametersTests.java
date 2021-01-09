package qub;

public interface QubDependenciesUpdateParametersTests
{
    static void test(TestRunner runner)
    {
        PreCondition.assertNotNull(runner, "runner");

        runner.testGroup(QubDependenciesUpdateParameters.class, () ->
        {
            runner.testGroup("constructor()", () ->
            {
                runner.test("with null output", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = null;
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(InMemoryCharacterToByteStream.create()).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    test.assertThrows(() -> new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("output cannot be null."));
                });

                runner.test("with null verbose", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = null;
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    test.assertThrows(() -> new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("verbose cannot be null."));
                });

                runner.test("with null folder", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final Folder folder = null;
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    test.assertThrows(() -> new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("folder cannot be null."));
                });

                runner.test("with null environmentVariables", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = null;
                    test.assertThrows(() -> new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("environmentVariables cannot be null."));
                });

                runner.test("with valid arguments", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);
                    test.assertSame(output, parameters.getOutput());
                    test.assertSame(verbose, parameters.getVerbose());
                    test.assertSame(folder, parameters.getFolder());
                    test.assertSame(environmentVariables, parameters.getEnvironmentVariables());
                });
            });
        });
    }
}