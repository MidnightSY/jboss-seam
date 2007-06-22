package org.jboss.seam.drools;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.drools.RuleBaseFactory;
import org.drools.compiler.DrlParser;
import org.drools.compiler.PackageBuilder;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.compiler.ParserError;
import org.drools.lang.descr.PackageDescr;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.ResourceLoader;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;

/**
 * Manager component for a Drools RuleBase
 * 
 * @author Gavin King
 *
 */
@Scope(ScopeType.APPLICATION)
@BypassInterceptors
public class RuleBase
{
   private static final LogProvider log = Logging.getLogProvider(RuleBase.class);
   
   private String[] ruleFiles;
   private String dslFile;
   private org.drools.RuleBase ruleBase;
   
   @Create
   public void compileRuleBase() throws Exception
   {
      PackageBuilderConfiguration conf = new PackageBuilderConfiguration();
      conf.setCompiler(PackageBuilderConfiguration.JANINO);
      PackageBuilder builder = new PackageBuilder(conf);
      
      if (ruleFiles!=null)
      {
         for (String ruleFile: ruleFiles)
         {
            log.info("parsing rules: " + ruleFile);
            InputStream stream = ResourceLoader.instance().getResourceAsStream(ruleFile);
            if (stream==null)
            {
               throw new IllegalStateException("could not locate rule file: " + ruleFile);
            }
            // read in the source
            Reader drlReader = new InputStreamReader(stream);
            PackageDescr packageDescr;
            DrlParser drlParser = new DrlParser();
            if (dslFile==null)
            {
               packageDescr = drlParser.parse(drlReader);
            }
            else
            {
               Reader dslReader = new InputStreamReader( ResourceLoader.instance().getResourceAsStream(dslFile) );
               packageDescr = drlParser.parse(drlReader, dslReader);
            }
            if ( drlParser.hasErrors() )
            {
               log.error("errors parsing rules in: " + ruleFile);
               for ( ParserError error: (List<ParserError>) drlParser.getErrors() )
               {
                  log.error( error.getMessage() + " (" + ruleFile + ':' + error.getRow() + ')' );
               }
            }
            // pre build the package
            builder.addPackage(packageDescr);
         }
      }
      
      // add the package to a rulebase
      ruleBase = RuleBaseFactory.newRuleBase();
      ruleBase.addPackage( builder.getPackage() );
   }
   
   @Unwrap
   public org.drools.RuleBase getRuleBase()
   {
      return ruleBase;
   }
   public String[] getRuleFiles()
   {
      return ruleFiles;
   }
   public void setRuleFiles(String[] ruleFiles)
   {
      this.ruleFiles = ruleFiles;
   }
   public String getDslFile()
   {
      return dslFile;
   }
   public void setDslFile(String dslFile)
   {
      this.dslFile = dslFile;
   }
}
