package to.wetransform.halecli.project

import org.eclipse.core.runtime.content.IContentType

import to.wetransform.halecli.CommandContext
import to.wetransform.halecli.project.advisor.SaveProjectAdvisor;
import eu.esdihumboldt.hale.common.align.model.Alignment
import eu.esdihumboldt.hale.common.core.io.HaleIO
import eu.esdihumboldt.hale.common.core.io.extension.IOProviderDescriptor
import eu.esdihumboldt.hale.common.core.io.project.ComplexConfigurationService;
import eu.esdihumboldt.hale.common.core.io.project.ProjectIO;
import eu.esdihumboldt.hale.common.core.io.project.ProjectWriter
import eu.esdihumboldt.hale.common.core.io.project.extension.ProjectFileExtension
import eu.esdihumboldt.hale.common.core.io.project.extension.ProjectFileFactory;
import eu.esdihumboldt.hale.common.core.io.project.model.IOConfiguration
import eu.esdihumboldt.hale.common.core.io.project.model.Project;
import eu.esdihumboldt.hale.common.core.io.project.model.ProjectFile
import eu.esdihumboldt.hale.common.core.io.report.IOReport
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier
import eu.esdihumboldt.hale.common.core.report.ReportHandler;
import eu.esdihumboldt.hale.common.core.service.ServiceProvider
import eu.esdihumboldt.hale.common.headless.HeadlessIO;
import eu.esdihumboldt.hale.common.headless.impl.HeadlessProjectAdvisor;
import eu.esdihumboldt.hale.common.headless.impl.ProjectTransformationEnvironment
import eu.esdihumboldt.hale.common.schema.model.SchemaSpace
import eu.esdihumboldt.util.io.OutputSupplier
import groovy.transform.CompileStatic;
import groovy.util.CliBuilder;

abstract class AbstractDeriveProjectCommand extends AbstractProjectEnvironmentCommand {
  
  static class DeriveProjectResult {
    Alignment alignment
    Project project
  }

  @Override
  void setupOptions(CliBuilder cli) {
    super.setupOptions(cli);
    
    cli._(longOpt: 'name', args: 1, argName: 'variant-name', 'Set the name of project variant')
  }
  
  abstract DeriveProjectResult deriveProject(ProjectTransformationEnvironment projectEnv,
    OptionAccessor options)
  
  @Override
  boolean runForProject(ProjectTransformationEnvironment projectEnv, URI projectLocation,
    OptionAccessor options, CommandContext context, ReportHandler reports) {
    
    def variant = options.name
    if (!variant) {
      variant = 'variant'
    }
    
    ComplexConfigurationService orgConf = ProjectIO.createProjectConfigService(projectEnv.project)
    if (orgConf.getBoolean('derivedProject', false)) {
      println 'Skipping derived project'
      return true
    }
    
    DeriveProjectResult result = deriveProject(projectEnv, options)
    if (!result) {
      return true
    }
    
    Project project = result.project
    ComplexConfigurationService conf = ProjectIO.createProjectConfigService(project)
    conf.setBoolean('derivedProject', true)
    
    //XXX only supported for files right now
    File projectFile = new File(projectLocation)
    
    String fileName = projectFile.name
    String extension = 'halex'
    int dotIndex = fileName.lastIndexOf('.')
    if (dotIndex > 0) {
      String ext = fileName[(dotIndex + 1)..-1]
      if (ext) extension = ext
      fileName = fileName[0..(dotIndex - 1)] + "-${variant}.$extension"
    }
    
    File projectOut = new File(projectFile.parentFile, fileName)
    def output = new FileIOSupplier(projectOut)
    
    saveProject(project, result.alignment, projectEnv.sourceSchema,
      projectEnv.targetSchema, output, reports, extension)
    
    true
  }
   
  @CompileStatic   
  void saveProject(Project project, Alignment alignment, SchemaSpace sourceSchema,
    SchemaSpace targetSchema, LocatableOutputSupplier<? extends OutputStream> output,
    ReportHandler reports, String extension) {
    
    // write project
    IContentType projectType = HaleIO.findContentType(
      ProjectWriter.class, null, "project.$extension")
    IOProviderDescriptor factory = HaleIO.findIOProviderFactory(
      ProjectWriter.class, projectType, null);
    ProjectWriter projectWriter
    try {
      projectWriter = (ProjectWriter) factory.createExtensionObject()
    } catch (Exception e1) {
      throw new IllegalStateException("Failed to create project writer", e1)
    }
    projectWriter.setTarget(output)

    // store (incomplete) save configuration
    IOConfiguration saveConf = new IOConfiguration()
    projectWriter.storeConfiguration(saveConf.getProviderConfiguration())
    saveConf.setProviderId(factory.getIdentifier())
    project.setSaveConfiguration(saveConf)

    SaveProjectAdvisor advisor = new SaveProjectAdvisor(project, alignment, sourceSchema,
      targetSchema);
    advisor.prepareProvider(projectWriter)
    advisor.updateConfiguration(projectWriter)
    // HeadlessIO.executeProvider(projectWriter, advisor, null, reports);
    IOReport report
    try {
      report = projectWriter.execute(null)
    } catch (Exception e) {
      throw new IllegalStateException("Error writing project file.", e)
    }
    if (report != null) {
      if (!report.isSuccess() || report.errors) {
        throw new IllegalStateException("Error writing project file.")
      }
    }
  }

}
