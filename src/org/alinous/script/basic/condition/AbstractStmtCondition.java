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
package org.alinous.script.basic.condition;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.alinous.debug.StepInCandidates;
import org.alinous.exec.ScriptCheckContext;
import org.alinous.exec.ScriptError;
import org.alinous.exec.pages.PostContext;
import org.alinous.expections.AlinousException;
import org.alinous.expections.ExecutionException;
import org.alinous.script.IScriptSentence;
import org.alinous.script.runtime.VariableRepository;
import org.jdom.Element;

public abstract class AbstractStmtCondition implements IStatementCondition
{
	protected List<IScriptCondition> conditions = new LinkedList<IScriptCondition>();
	
	protected IScriptSentence callerSentence;

	private int line;
	private int linePosition;
	private String filePath;
	
	public void addCondition(IScriptCondition condition)
	{
		condition.setCallerSentence(this.callerSentence);
		this.conditions.add(condition);
	}
	
	public boolean evaluate(PostContext context, VariableRepository valRepo) throws ExecutionException
	{
		throw new ExecutionException("This condition is not condition but a statement.");
	}
	
	public void exportIntoJDomElement(Element parent) throws AlinousException
	{
		Element andElement = new Element(IScriptCondition.TAG_CONDITION);
		andElement.setAttribute(IScriptCondition.ATTR_COND_CLASS, this.getClass().getName());
		
		parent.addContent(andElement);
		
		Iterator<IScriptCondition> it = this.conditions.iterator();
		while(it.hasNext()){
			IScriptCondition cnd = it.next();
			cnd.exportIntoJDomElement(andElement);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void importFromJDomElement(Element element) throws AlinousException
	{
		List<Element> list = element.getChildren(IScriptCondition.TAG_CONDITION);
		
		Iterator<Element> it = list.iterator();
		while(it.hasNext()){
			Element el = it.next();
			
			IScriptCondition cnd = JDomConditionFactory.createConditionFromJDomElement(el);
			cnd.importFromJDomElement(el);
			addCondition(cnd);
		}

	}
	
	public void canStepInStatements(StepInCandidates candidates)
	{
		// debug
		//AlinousDebug.debugOut("AbstractStmtCondition#canStepInStatements : " + this);
		
		Iterator<IScriptCondition> it = this.conditions.iterator();
		while(it.hasNext()){
			IScriptCondition cnd = it.next();
			
			cnd.canStepInStatements(candidates);
		}

	}

	public void setCallerSentence(IScriptSentence callerSentence)
	{
		this.callerSentence = callerSentence;
		
		Iterator<IScriptCondition> it = this.conditions.iterator();
		while(it.hasNext()){
			IScriptCondition cnd = it.next();
			cnd.setCallerSentence(callerSentence);
		}
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
		
		Iterator<IScriptCondition> it = this.conditions.iterator();
		while(it.hasNext()){
			it.next().setFilePath(filePath);
		}
	}
	
	public void checkStaticErrors(ScriptCheckContext scContext,
			List<ScriptError> errorList)
	{
		Iterator<IScriptCondition> it = this.conditions.iterator();
		while(it.hasNext()){
			IScriptCondition cnd = it.next();
			cnd.checkStaticErrors(scContext, errorList);
		}
	}
	
}
