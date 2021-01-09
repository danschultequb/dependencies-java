package qub;

public interface QubDependenciesListTests
{
    static void test(TestRunner runner)
    {
        PreCondition.assertNotNull(runner, "runner");

        runner.testGroup(QubDependenciesList.class, () ->
        {
            runner.testGroup("getParameters(Process)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependenciesList.getParameters(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with no arguments", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create())
                    {
                        final QubDependenciesListParameters parameters = QubDependenciesList.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertSame(process.getOutputWriteStream(), parameters.getOutput());
                        test.assertEqual("", process.getOutputWriteStream().getText().await());
                        test.assertNotNull(parameters.getVerbose());
                        test.assertEqual(process.getCurrentFolder(), parameters.getFolder());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("-?"))
                    {
                        final QubDependenciesListParameters parameters = QubDependenciesList.getParameters(process);
                        test.assertNull(parameters);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-dependencies list [--profiler] [--verbose] [--help]",
                                "  List the dependencies of a project.",
                                "  --profiler:   Whether or not this application should pause before it is run to allow a profiler to be attached.",
                                "  --verbose(v): Whether or not to show verbose logs.",
                                "  --help(?):    Show the help message for this application."
                            ),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                    }
                });
            });

            runner.testGroup("run(QubDependenciesParameters)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependenciesList.run(null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("with non-existing folder", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output)
                        .setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.getFolder("/project/").await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "No project.json file found at /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with no project.json", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output)
                        .setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "No project.json file found at /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no java property", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output)
                        .setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "No \"java\" property found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no dependencies property", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output)
                        .setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "No dependencies found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with empty dependencies property", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output)
                        .setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create()))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "No dependencies found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no QUB_HOME environment variable", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"),
                                ProjectSignature.create("d", "e", "f"),
                                ProjectSignature.create("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create();
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Can't discover transitive dependencies if a QUB_HOME environment variable is not specified."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with relative QUB_HOME environment variable", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"),
                                ProjectSignature.create("d", "e", "f"),
                                ProjectSignature.create("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", "qub");
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Can't discover transitive dependencies if the QUB_HOME environment variable is not rooted."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with one dependency", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "c").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("c")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@c"),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with two dependencies", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "c").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("c")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "f").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("f")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"),
                                ProjectSignature.create("d", "e", "f"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 2 dependencies:",
                            "  a/b@c",
                            "  d/e@f"),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with three dependencies", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "c").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("c")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "f").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("f")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("g", "h", "i").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("g")
                            .setProject("h")
                            .setVersion("i")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"),
                                ProjectSignature.create("d", "e", "f"),
                                ProjectSignature.create("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 3 dependencies:",
                            "  a/b@c",
                            "  d/e@f",
                            "  g/h@i"),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with one grandchild dependency", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "c").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("c")
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    ProjectSignature.create("d", "e", "f"))))
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "f").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("f")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@c",
                            "    d/e@f"),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with two grandchild dependencies", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "c").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("c")
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    ProjectSignature.create("d", "e", "f"),
                                    ProjectSignature.create("g", "h", "i"))))
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "f").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("f")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("g", "h", "i").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("g")
                            .setProject("h")
                            .setVersion("i")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@c",
                            "    d/e@f",
                            "    g/h@i"),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with one dependency that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@c - Not Found"),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with one dependency that doesn't has a Java property", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final VerboseCharacterToByteWriteStream verbose = new VerboseCharacterToByteWriteStream(output).setIsVerbose(false);
                    final InMemoryFileSystem fileSystem = InMemoryFileSystem.create(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "c").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("c")
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                ProjectSignature.create("a", "b", "c"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = EnvironmentVariables.create()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesListParameters parameters = new QubDependenciesListParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesList.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Getting dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@c - No Java Property"),
                        Strings.getLines(output.getText().await()));
                });
            });
        });
    }
}