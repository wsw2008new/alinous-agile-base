/**
 * CROSSFIRE JAPAN INCORPORATED
 * This source code is under GPL License.
 * info@crossfire.jp
 * Official web site
 * http://alinous.org
 * 
 *  Copyright (C) 2007 Tomohiro Iizuka
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.alinous.script.sql;

import java.util.List;

import org.alinous.AlinousCore;
import org.alinous.datasrc.AlinousDataSourceManager;
import org.alinous.datasrc.DataSrcConnection;
import org.alinous.datasrc.exception.DataSourceException;
import org.alinous.datasrc.types.TypeHelper;
import org.alinous.debug.StepInCandidates;
import org.alinous.exec.IExecutable;
import org.alinous.exec.ScriptCheckContext;
import org.alinous.exec.ScriptError;
import org.alinous.exec.pages.PostContext;
import org.alinous.expections.ExecutionException;
import org.alinous.script.AlinousScript;
import org.alinous.script.ISQLSentence;
import org.alinous.script.runtime.IScriptVariable;
import org.alinous.script.runtime.VariableRepository;
import org.alinous.script.sql.adjustopt.AdjustSet;
import org.alinous.script.sql.adjustopt.AdjustWhere;
import org.alinous.script.sql.other.TableIdentifier;
import org.alinous.script.statement.FunctionCall;
import org.alinous.test.coverage.FileCoverage;
import org.alinous.test.coverage.LineCoverage;
import org.jdom.Element;

public class UpdateSentence implements ISQLSentence
{
	private TableIdentifier table;
	private SetClause set;
	private WhereClause where;
	private AdjustWhere adjustWhere = null;
	private AdjustSet adjustSet = null;
	
	private int line;
	private int linePosition;
	private String filePath;
	private boolean precompilable = true;
	
	public UpdateSentence()
	{
		this.adjustWhere = new AdjustWhere();
	}
	
	public SetClause getSet()
	{
		return set;
	}
	
	public void setSet(SetClause set)
	{
		this.set = set;
	}
	
	public TableIdentifier getTable()
	{
		return table;
	}
	
	public void setTable(TableIdentifier table)
	{
		this.table = table;
	}
	
	public WhereClause getWhere()
	{
		return where;
	}
	
	public void setWhere(WhereClause where)
	{
		this.where = where;
	}
	
	public AdjustSet getAdjustSet()
	{
		return adjustSet;
	}

	public void setAdjustSet(AdjustSet adjustSet)
	{
		this.adjustSet = adjustSet;
	}

	public AdjustWhere getAdjustWhere()
	{
		return adjustWhere;
	}

	public void setAdjustWhere(AdjustWhere adjustWhere)
	{
		this.adjustWhere = adjustWhere;
	}

	public void setDataSourceManager(AlinousDataSourceManager dataSourceManager)
	{
	}

	public boolean execute(PostContext context, VariableRepository valRepo)
		throws ExecutionException
	{
		context.getCore().reporttExecuted(this);
		
		String dataSrc = context.getDataSrc();
		if(dataSrc == null){
			dataSrc = context.getCore().getConfig().getSystemRepositoryConfig().getDefaultSrc();
		}

		if(this.set == null || !this.set.isReady(context, valRepo, this.adjustWhere)){
			return true;
		}
		
		DataSrcConnection con = null;
		try {
			con = context.getUnit().getConnectionManager().connect(dataSrc, context);
		} catch (DataSourceException e) {
			throw new ExecutionException(e, "");
		} catch(Throwable e){
			throw new ExecutionException(e, "execute update failed on getting connection");
		}
		
		
		try {
			if(AlinousCore.debug(context)){
				con.setOutSql(true);
			}
			
			context.getPrecompile().setSqlSentence(this);
			con.update(this.table.extract(context, valRepo, null, null, null), this.set, this.where, context, valRepo, this.adjustWhere, this.adjustSet);
			context.getPrecompile().setSqlSentence(null);
			
			con.setOutSql(false);
		} catch (DataSourceException e) {
			con.close();
			throw new ExecutionException(e, "");
		} catch(Throwable e){
			throw new ExecutionException(e, "execute update failed");
		}
		
		return true;
	}
	
	public int getLine()
	{
		return line;
	}
	
	public void setLine(int line)
	{
		this.line = line;
	}
	
	public int getLinePosition()
	{
		return linePosition;
	}
	
	public void setLinePosition(int linePosition)
	{
		this.linePosition = linePosition;
	}
	
	public String getFilePath()
	{
		return filePath;
	}
	
	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}
	
	public void exportIntoJDomElement(Element parent)
	{
		Element selectElement = new Element(IExecutable.TAG_EXECUTABLE);
		
		selectElement.setAttribute(IExecutable.ATTR_CLASS, this.getClass().getName());
		
		parent.addContent(selectElement);
		
	}
	
	public void importFromJDomElement(Element element)
	{
		
	}
	
	public StepInCandidates getStepInCandidates()
	{
		return null;
	}

	public IScriptVariable getReturnedVariable(PostContext context)
	{
		return context.getReturnedVariable(this);
	}

	public String extract(PostContext context, VariableRepository providor, AdjustWhere adjustWhere,
			AdjustSet adjustSet, TypeHelper helper) throws ExecutionException
	{
		return null;
	}

	public boolean isReady(PostContext context, VariableRepository providor, AdjustWhere adjustWhere) throws ExecutionException
	{
		return false;
	}
	
	public boolean isPrecompilable()
	{
		return precompilable;
	}
	public void setPrecompilable(boolean b)
	{
		this.precompilable = b;
	}

	public void checkStaticErrors(ScriptCheckContext scContext,
			List<ScriptError> errorList)
	{
		if(scContext.isSkipOnce()){
			scContext.setSkipOnce(false);
			return;
		}
		
	}

	@Override
	public void getFunctionCall(ScriptCheckContext scContext, List<FunctionCall> call, AlinousScript script)
	{
		
	}
	
	@Override
	public void setupCoverage(FileCoverage coverage)
	{
		LineCoverage lineCoverage = new LineCoverage(this.line, this.linePosition, this);
		coverage.addLineCoverage(lineCoverage);
		
	}
}
