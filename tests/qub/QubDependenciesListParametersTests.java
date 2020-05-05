package qub;

public interface QubDependenciesListParametersTests
{
    static void test(TestRunner runner)
    {
        PreCondition.assertNotNull(runner, "runner");

        runner.testGroup(QubDependenciesListParameters.class, () ->
        {
            runner.testGroup("constructor()", () ->
            {
                runner.test("with null output", (Test test) ->
                {
                    final InMemoryCharacterStream output = null;
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, InMemoryCharacterStream.create());
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    test.assertThrows(() -> new QubDependenciesListParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("output cannot be null."));
                });

                runner.test("with null verbose", (Test test) ->
                {
                    final InMemoryCharacterStream output = InMemoryCharacterStream.create();
                    final VerboseCharacterWriteStream verbose = null;
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    test.assertThrows(() -> new QubDependenciesListParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("verbose cannot be null."));
                });

                runner.test("with null folder", (Test test) ->
                {
                    final InMemoryCharacterStream output = InMemoryCharacterStream.create();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final Folder folder = null;
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    test.assertThrows(() -> new QubDependenciesListParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("folder cannot be null."));
                });

                runner.test("with null environmentVariables", (Test test) ->
                {
                    final InMemoryCharacterStream output = InMemoryCharacterStream.create();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = null;
                    test.assertThrows(() -> new QubDependenciesListParameters(output, verbose, folder, environmentVariables),
                        new PreConditionFailure("environmentVariables cannot be null."));
                });

                runner.test("with valid arguments", (Test test) ->
                {
                    final InMemoryCharacterStream output = InMemoryCharacterStream.create();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    final Folder folder = fileSystem.getFolder("/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);
                    test.assertSame(output, parameters.getOutput());
                    test.assertSame(verbose, parameters.getVerbose());
                    test.assertSame(folder, parameters.getFolder());
                    test.assertSame(environmentVariables, parameters.getEnvironmentVariables());
                });
            });
        });
    }
}