package qub;

public interface QubDependenciesUpdateTests
{
    static void test(TestRunner runner)
    {
        PreCondition.assertNotNull(runner, "runner");

        runner.testGroup(QubDependenciesUpdate.class, () ->
        {
            runner.testGroup("getParameters(Process)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependenciesUpdate.getParameters(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with no arguments", (Test test) ->
                {
                    try (final Process process = Process.create())
                    {
                        final InMemoryCharacterStream output = new InMemoryCharacterStream();
                        process.setOutputCharacterWriteStream(output);

                        final QubDependenciesUpdateParameters parameters = QubDependenciesUpdate.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertSame(output, parameters.getOutput());
                        test.assertEqual("", output.getText().await());
                        test.assertNotNull(parameters.getVerbose());
                        test.assertEqual(process.getCurrentFolder().await(), parameters.getFolder());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    try (final Process process = Process.create("-?"))
                    {
                        final InMemoryCharacterStream output = new InMemoryCharacterStream();
                        process.setOutputCharacterWriteStream(output);

                        final QubDependenciesUpdateParameters parameters = QubDependenciesUpdate.getParameters(process);
                        test.assertNull(parameters);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-dependencies update [--profiler] [--verbose] [--help]",
                                "  Update the dependencies of a project.",
                                "  --profiler: Whether or not this application should pause before it is run to allow a profiler to be attached.",
                                "  --verbose(v): Whether or not to show verbose logs.",
                                "  --help(?): Show the help message for this application."
                            ),
                            Strings.getLines(output.getText().await()));
                    }
                });
            });

            runner.testGroup("run(QubDependenciesParameters)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependenciesUpdate.run(null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("with non-existing folder", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.getFolder("/project/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No project.json file found at /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with no project.json", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No project.json file found at /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no java property", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No \"java\" property found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no dependencies property", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava())
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No dependencies found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with empty dependencies property", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create()))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No dependencies found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no QUB_HOME environment variable", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "c"),
                                new ProjectSignature("d", "e", "f"),
                                new ProjectSignature("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Can't discover transitive dependencies if a QUB_HOME environment variable is not specified."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with relative QUB_HOME environment variable", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "c"),
                                new ProjectSignature("d", "e", "f"),
                                new ProjectSignature("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", "qub");
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Can't discover transitive dependencies if the QUB_HOME environment variable is not rooted."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with three dependencies with mixed updates", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = new QubFolder(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("a", "b", "2").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("2")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "3").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("3")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("g", "h", "4").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("g")
                            .setProject("h")
                            .setVersion("4")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("g", "h", "5").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("g")
                            .setProject("h")
                            .setVersion("5")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"),
                                new ProjectSignature("d", "e", "3"),
                                new ProjectSignature("g", "h", "4"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 3 dependencies:",
                            "  a/b@1 - Updated to a/b@2",
                            "  d/e@3 - No Updates",
                            "  g/h@4 - Updated to g/h@5"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        new ProjectJSON()
                            .setJava(new ProjectJSONJava()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "2"),
                                    new ProjectSignature("d", "e", "3"),
                                    new ProjectSignature("g", "h", "5")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with one up-to-date dependency", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = new QubFolder(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(new ProjectJSONJava()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("d", "e", "1"))))
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No Updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        new ProjectJSON()
                            .setJava(new ProjectJSONJava()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with two up-to-date dependencies", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = new QubFolder(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "1").await().setContentsAsString(
                        new ProjectJSON()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("1")
                            .setJava(new ProjectJSONJava())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"),
                                new ProjectSignature("d", "e", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 2 dependencies:",
                            "  a/b@1 - No Updates",
                            "  d/e@1 - No Updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        new ProjectJSON()
                            .setJava(new ProjectJSONJava()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1"),
                                    new ProjectSignature("d", "e", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with one dependency that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = new QubFolder(rootFolder.getFolder("qub").await());
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", new ProjectJSON()
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - Not Found"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        new ProjectJSON()
                            .setJava(new ProjectJSONJava()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });
            });
        });
    }
}