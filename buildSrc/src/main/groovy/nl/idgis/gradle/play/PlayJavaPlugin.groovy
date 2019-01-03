package nl.idgis.gradle.play;

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.play.plugins.PlayPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.plugins.ide.eclipse.EclipsePlugin

class PlayJavaPlugin implements Plugin<Project> {
	void apply  (Project project) {
		// Apply the default Play plugin and the Java base plugin to provide basic
		// build functionality which will be extended by this plugin.
		project.pluginManager.apply (JavaBasePlugin)
		project.pluginManager.apply (PlayPlugin)
		
		project.afterEvaluate {
			// Configure the eclipse plugin if present: add Play! source directories and
			// set the correct project nature.
			// Configure the Eclipse plugin if present: the QueryDSL generated source directory is
			// added as a source folder.
			project.plugins.withType (EclipsePlugin) {
				project.eclipse {
					classpath {
						plusConfigurations += [ project.configurations.play ]
						
						file {
							whenMerged { cp ->
								cp.entries.add (new org.gradle.plugins.ide.eclipse.model.SourceFolder('app', null))
								cp.entries.add (new org.gradle.plugins.ide.eclipse.model.SourceFolder('test', null))
								cp.entries.add (new org.gradle.plugins.ide.eclipse.model.SourceFolder("build/playBinary/src/compilePlayBinaryRoutes", null))
								cp.entries.add (new org.gradle.plugins.ide.eclipse.model.SourceFolder("build/playBinary/src/compilePlayBinaryTwirlTemplates", null))
							}
						}
					}
					
					it.project {
						natures 'org.scala-ide.sdt.core.scalanature', 'org.eclipse.jdt.core.javanature'
					}
				}
			}
		}
	}	
}