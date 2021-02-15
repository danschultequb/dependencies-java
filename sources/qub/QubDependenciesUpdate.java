package qub;

public interface QubDependenciesUpdate
{
    String actionName = "update";
    String actionDescription = "Update the dependencies of a project.";

    static QubDependenciesUpdateParameters getParameters(DesktopProcess process)
    {
        PreCondition.assertNotNull(process, "process");

        QubDependenciesUpdateParameters result = null;

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName(QubDependencies.getActionFullName(QubDependenciesUpdate.actionName))
            .setApplicationDescription(QubDependenciesUpdate.actionDescription);
        final CommandLineParameterBoolean intellijParameter = parameters.addBoolean("intellij", true)
            .setDescription("Whether or not to update IntelliJ project files.");
        final CommandLineParameterProfiler profilerParameter = parameters.addProfiler(process, QubDependenciesUpdate.class);
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterHelp helpParameter = parameters.addHelp();

        if (!helpParameter.showApplicationHelpLines(process).await())
        {
            profilerParameter.await();

            final CharacterWriteStream output = process.getOutputWriteStream();
            final VerboseCharacterToByteWriteStream verbose = verboseParameter.getVerboseCharacterToByteWriteStream().await();
            final Folder folder = process.getCurrentFolder();
            final EnvironmentVariables environmentVariables = process.getEnvironmentVariables();
            final boolean intellij = intellijParameter.getValue().await();

            result = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                .setIntellij(intellij);
        }

        return result;
    }

    static int run(QubDependenciesUpdateParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");

        int exitCode = 0;

        final CharacterWriteStream output = parameters.getOutput();
        final VerboseCharacterToByteWriteStream verbose = parameters.getVerbose();
        final Folder folder = parameters.getFolder();
        final EnvironmentVariables environmentVariables = parameters.getEnvironmentVariables();
        final boolean intellij = parameters.getIntellij();

        output.writeLine("Updating dependencies for " + folder + "...").await();

        final File projectJsonFile = folder.getFile("project.json").await();
        final ProjectJSON projectJSON = ProjectJSON.parse(projectJsonFile)
            .catchError(FileNotFoundException.class)
            .await();
        if (projectJSON == null)
        {
            output.writeLine("No project.json file found at " + projectJsonFile + ".").await();
            exitCode = 1;
        }
        else
        {
            final ProjectJSONJava projectJSONJava = projectJSON.getJava();
            if (projectJSONJava == null)
            {
                output.writeLine("No \"java\" property found in " + projectJsonFile + ".").await();
                exitCode = 1;
            }
            else
            {
                final String qubHome = environmentVariables.get("QUB_HOME")
                    .catchError(NotFoundException.class)
                    .await();
                if (Strings.isNullOrEmpty(qubHome))
                {
                    output.writeLine("Can't discover transitive dependencies if a QUB_HOME environment variable is not specified.").await();
                    exitCode = 1;
                }
                else
                {
                    final Path qubHomePath = Path.parse(qubHome);
                    if (!qubHomePath.isRooted())
                    {
                        output.writeLine("Can't discover transitive dependencies if the QUB_HOME environment variable is not rooted.").await();
                        exitCode = 1;
                    }
                    else
                    {
                        final QubFolder qubFolder = QubFolder.get(folder.getFileSystem().getFolder(qubHomePath).await());
                        final Iterable<ProjectSignature> dependencies = projectJSONJava.getDependencies();
                        final int dependencyCount = dependencies.getCount();
                        output.writeLine("Found " + dependencyCount + " " + (dependencyCount == 1 ? "dependency" : "dependencies") + (dependencyCount == 0 ? "." : ":")).await();
                        final IndentedCharacterWriteStream indentedOutput = IndentedCharacterWriteStream.create(output);
                        final List<ProjectSignature> newDependencies = List.create();
                        indentedOutput.indent(() ->
                        {
                            boolean dependenciesChanged = false;
                            for (final ProjectSignature dependency : dependencies)
                            {
                                indentedOutput.write(dependency.toString()).await();

                                final QubProjectFolder projectFolder = qubFolder.getProjectFolder(dependency.getPublisher(), dependency.getProject()).await();
                                final QubProjectVersionFolder latestVersionFolder = projectFolder.getLatestProjectVersionFolder().catchError().await();
                                if (latestVersionFolder == null)
                                {
                                    newDependencies.add(dependency);
                                    indentedOutput.writeLine(" - Not Found").await();
                                }
                                else
                                {
                                    final VersionNumber latestVersion = latestVersionFolder.getVersion().await();
                                    if (Comparer.equal(dependency.getVersion(), latestVersion))
                                    {
                                        newDependencies.add(dependency);
                                        indentedOutput.writeLine(" - No updates").await();
                                    }
                                    else
                                    {
                                        final ProjectSignature newDependency = ProjectSignature.create(dependency.getPublisher(), dependency.getProject(), latestVersion);
                                        newDependencies.add(newDependency);
                                        dependenciesChanged = true;
                                        indentedOutput.writeLine(" - Updated to " + newDependency).await();
                                    }
                                }
                            }

                            if (dependenciesChanged)
                            {
                                projectJSONJava.setDependencies(newDependencies);
                                projectJsonFile.setContentsAsString(projectJSON.toString(JSONFormat.pretty)).await();
                            }
                        });

                        if (intellij)
                        {
                            final Iterable<ProjectSignature> projectJsonTransitiveDependencies = projectJSONJava.getTransitiveDependencies(qubFolder).toList();

                            final Iterable<File> intellijProjectFiles = folder.getFilesRecursively().await()
                                .where((File file) -> Comparer.equal(file.getFileExtension(), ".iml"));
                            if (intellijProjectFiles.any())
                            {
                                indentedOutput.writeLine("Updating IntelliJ module files...").await();
                                indentedOutput.indent(() ->
                                {
                                    for (final File intellijProjectFile : intellijProjectFiles)
                                    {
                                        final IntellijModule intellijModule = IntellijModule.parse(intellijProjectFile)
                                            .catchError(() -> indentedOutput.writeLine("Invalid Intellij Module file: " + intellijProjectFile).await())
                                            .await();
                                        if (intellijModule != null)
                                        {
                                            final List<ProjectSignature> dependenciesToAddToModule = List.create(projectJsonTransitiveDependencies);
                                            final Iterable<IntellijModuleLibrary> currentModuleLibraries = intellijModule.getModuleLibraries().toList();

                                            intellijModule.clearModuleLibraries();

                                            for (final IntellijModuleLibrary moduleLibrary : currentModuleLibraries)
                                            {
                                                final String classesUrl = moduleLibrary.getClassesUrls().first();
                                                if (Strings.isNullOrEmpty(classesUrl) || !classesUrl.startsWith("jar://"))
                                                {
                                                    intellijModule.addModuleLibrary(moduleLibrary);
                                                }
                                                else
                                                {
                                                    final int startIndex = "jar://".length();
                                                    int endIndex = classesUrl.length();
                                                    if (classesUrl.endsWith("!/"))
                                                    {
                                                        endIndex -= "!/".length();
                                                    }
                                                    final Path compiledSourcesFilePath = Path.parse(classesUrl.substring(startIndex, endIndex));
                                                    if (!qubFolder.isAncestorOf(compiledSourcesFilePath).await())
                                                    {
                                                        indentedOutput.writeLine(compiledSourcesFilePath + " - No updates").await();
                                                        intellijModule.addModuleLibrary(moduleLibrary);
                                                    }
                                                    else
                                                    {
                                                        final Path compiledSourcesRelativeFilePath = compiledSourcesFilePath.relativeTo(qubFolder);
                                                        final Indexable<String> segments = compiledSourcesRelativeFilePath.getSegments();
                                                        final String publisher = segments.get(0);
                                                        final String project = segments.get(1);
                                                        String version = segments.get(2);
                                                        if (version.equals("versions"))
                                                        {
                                                            version = segments.get(3);
                                                        }
                                                        final ProjectSignature currentQubDependency = ProjectSignature.create(publisher, project, version);

                                                        final ProjectSignature newQubDependency = dependenciesToAddToModule.removeFirst(currentQubDependency::equalsIgnoreVersion);
                                                        if (newQubDependency == null)
                                                        {
                                                            indentedOutput.writeLine(currentQubDependency + " - Removed").await();
                                                        }
                                                        else
                                                        {
                                                            if (newQubDependency.equals(currentQubDependency))
                                                            {
                                                                indentedOutput.writeLine(currentQubDependency + " - No updates").await();
                                                            }
                                                            else
                                                            {
                                                                indentedOutput.writeLine(currentQubDependency + " - Updated to " + newQubDependency).await();
                                                            }

                                                            final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder(
                                                                newQubDependency.getPublisher(),
                                                                newQubDependency.getProject(),
                                                                newQubDependency.getVersion()).await();
                                                            intellijModule.addModuleLibrary(IntellijModuleLibrary.create()
                                                                .addClassesUrl("jar://" + projectVersionFolder.getCompiledSourcesFile().await().toString() + "!/")
                                                                .addSourcesUrl("jar://" + projectVersionFolder.getSourcesFile().await().toString() + "!/"));
                                                        }
                                                    }
                                                }
                                            }

                                            for (final ProjectSignature dependencyToAddToModule : dependenciesToAddToModule)
                                            {
                                                indentedOutput.writeLine(dependencyToAddToModule + " - Added").await();

                                                final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder(
                                                    dependencyToAddToModule.getPublisher(),
                                                    dependencyToAddToModule.getProject(),
                                                    dependencyToAddToModule.getVersion())
                                                    .await();
                                                intellijModule.addModuleLibrary(IntellijModuleLibrary.create()
                                                    .addClassesUrl("jar://" + projectVersionFolder.getCompiledSourcesFile().await().toString() + "!/")
                                                    .addSourcesUrl("jar://" + projectVersionFolder.getSourcesFile().await().toString() + "!/"));
                                            }

                                            intellijProjectFile.setContentsAsString(intellijModule.toString(XMLFormat.pretty)).await();
                                        }
                                    }
                                });
                            }

                            final File intellijWorkspaceFile = folder.getFile(".idea/workspace.xml").await();
                            if (intellijWorkspaceFile.exists().await())
                            {
                                indentedOutput.writeLine("Updating IntelliJ workspace file...").await();
                                indentedOutput.indent(() ->
                                {
                                    final IntellijWorkspace intellijWorkspace = Result.create(() -> IntellijWorkspace.create(XML.parse(intellijWorkspaceFile).await()))
                                        .catchError(() -> indentedOutput.writeLine("Invalid Intellij Workspace file: " + intellijWorkspaceFile).await())
                                        .await();

                                    final Folder testsFolder = folder.getFolder("tests").await();
                                    final Iterable<String> fullTestClassNames = testsFolder.getFilesRecursively().await()
                                        .where((File file) -> Comparer.equal(".java", file.getFileExtension()))
                                        .map((File testFile) -> testFile.relativeTo(testsFolder).withoutFileExtension().toString().replace('/', '.').replace('\\', '.'))
                                        .toList();

                                    final List<String> fullTestClassNamesToAdd = List.create(fullTestClassNames);

                                    final QubProjectFolder qubTestProjectFolder = qubFolder.getProjectFolder("qub", "test-java").await();
                                    final QubProjectVersionFolder qubTestLatestProjectVersionFolder = qubTestProjectFolder.getLatestProjectVersionFolder().await();
                                    final ProjectJSON qubTestProjectJson = ProjectJSON.parse(qubTestLatestProjectVersionFolder.getProjectJSONFile().await()).await();
                                    final ProjectJSONJava qubTestProjectJsonJava = qubTestProjectJson.getJava();
                                    final ProjectSignature qubTestProjectSignature = ProjectSignature.create("qub", "test-java", qubTestProjectJson.getVersion());
                                    final List<ProjectSignature> runConfigurationDependencies = List.create(qubTestProjectSignature)
                                        .addAll(qubTestProjectJsonJava.getTransitiveDependencies(qubFolder));

                                    for (final ProjectSignature projectJsonTransitiveDependency : projectJsonTransitiveDependencies)
                                    {
                                        runConfigurationDependencies.removeFirst(projectJsonTransitiveDependency::equalsIgnoreVersion);
                                        runConfigurationDependencies.add(projectJsonTransitiveDependency);
                                    }

                                    final CharacterList vmParameters = CharacterList.create();
                                    vmParameters.addAll("-classpath $PROJECT_DIR$/outputs");
                                    for (final ProjectSignature runConfigurationDependency : runConfigurationDependencies)
                                    {
                                        vmParameters.add(';');
                                        vmParameters.addAll(qubFolder.getCompiledSourcesFile(
                                            runConfigurationDependency.getPublisher(),
                                            runConfigurationDependency.getProject(),
                                            runConfigurationDependency.getVersion())
                                            .await()
                                            .toString());
                                    }
                                    final String vmParametersString = vmParameters.toString(true);
                                    final String outputFolderProgramParameter = "--output-folder=$PROJECT_DIR$/outputs";
                                    final String testJsonProgramParameter = "--testjson=false";

                                    final List<IntellijWorkspaceRunConfiguration> runConfigurationsToRemove = List.create();
                                    for (final IntellijWorkspaceRunConfiguration runConfiguration : intellijWorkspace.getRunConfigurations())
                                    {
                                        final String runConfigurationName = runConfiguration.getName();
                                        if (!fullTestClassNames.contains(runConfigurationName))
                                        {
                                            runConfigurationsToRemove.add(runConfiguration);
                                        }
                                        else
                                        {
                                            fullTestClassNamesToAdd.remove(runConfigurationName);

                                            runConfiguration.setType("Application");
                                            runConfiguration.setFactoryName("Application");
                                            runConfiguration.setMainClassFullName("qub.ConsoleTestRunner");
                                            runConfiguration.setModuleName(projectJSON.getProject());
                                            runConfiguration.setProgramParameters(Strings.join(' ', Iterable.create(outputFolderProgramParameter, testJsonProgramParameter, runConfigurationName)));
                                            runConfiguration.setVmParameters(vmParametersString);
                                        }
                                    }

                                    for (final IntellijWorkspaceRunConfiguration runConfigurationToRemove : runConfigurationsToRemove)
                                    {
                                        intellijWorkspace.removeRunConfiguration(runConfigurationToRemove);
                                    }

                                    for (final String fullTestClassNameToAdd : fullTestClassNamesToAdd)
                                    {
                                        intellijWorkspace.addRunConfiguration(IntellijWorkspaceRunConfiguration.create()
                                            .setName(fullTestClassNameToAdd)
                                            .setType("Application")
                                            .setFactoryName("Application")
                                            .setMainClassFullName("qub.ConsoleTestRunner")
                                            .setModuleName(projectJSON.getProject())
                                            .setProgramParameters(Strings.join(' ', Iterable.create(outputFolderProgramParameter, testJsonProgramParameter, fullTestClassNameToAdd)))
                                            .setVmParameters(vmParametersString));
                                    }

                                    intellijWorkspaceFile.setContentsAsString(intellijWorkspace.toString(XMLFormat.pretty)).await();
                                });
                            }
                        }
                    }
                }
            }
        }

        return exitCode;
    }
}
