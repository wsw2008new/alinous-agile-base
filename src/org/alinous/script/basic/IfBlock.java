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
package org.alinous.script.basic;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alinous.debug.StepInCandidates;
import org.alinous.exec.IExecutable;
import org.alinous.exec.ScriptCheckContext;
import org.alinous.exec.ScriptError;
import org.alinous.exec.pages.PostContext;
import org.alinous.expections.AlinousException;
import org.alinous.expections.ExecutionException;
import org.alinous.expections.RedirectRequestException;
import org.alinous.script.AlinousScript;
import org.alinous.script.IScriptSentence;
import org.alinous.script.basic.condition.IScriptCondition;
import org.alinous.script.basic.condition.JDomConditionFactory;
import org.alinous.script.runtime.VariableRepository;
import org.alinous.script.statement.FunctionCall;
import org.jdom.Element;

public class IfBlock extends AbstractScriptBlock
{
	public IfBlock(String filePath) {
		super(filePath);
	}
	public IfBlock()
	{
		super();
	}
	
	public static final String BLOCK_NAME = "IfBlock";

	private IScriptCondition condition;
	//private boolean condResult;
	
	private List<ElseBlock> elseBlockList = new ArrayList<ElseBlock>();
	

	
	public boolean execute(PostContext context, VariableRepository valRepo) throws ExecutionException, RedirectRequestException
	{
		this.condition.setCallerSentence(this);
		boolean condResult = this.condition.evaluate(context, valRepo);
		context.registerIfResult(this, condResult);
		
		if(!condResult){
			return handleElse(context, valRepo);
		}
		
		Iterator<IScriptSentence> it = this.sentences.iterator();
		while(it.hasNext()){
			IScriptSentence exec = it.next();
			
			boolean blRes = executeSentence(exec, context, valRepo);			
			
			if(!blRes){
				return false;
			}
			
			// Break and continue
			if(exec instanceof AbstractScriptBlock){
				AbstractScriptBlock block = (AbstractScriptBlock)exec ;
				if(block.getLastSentence(context) instanceof BreakSentence){
					break;
				}
				else if(block.getLastSentence(context) instanceof ContinueSentence){
					break;
				}
			}
			if(exec instanceof BreakSentence){
				break;
			}
			else if(exec instanceof ContinueSentence){
				break;
			}
			
		}
		
		
		return true;
	}
	
	private boolean handleElse(PostContext context, VariableRepository valRepo)
				throws ExecutionException, RedirectRequestException
	{
		Iterator<ElseBlock> it = this.elseBlockList.iterator();
		
		boolean result = true;
		while(it.hasNext()){
			ElseBlock elseBlk = it.next();
			
			try{
				result = executeSentence(elseBlk, context, valRepo);
			}catch(ExecutionException e){
				e.addStackTrace(elseBlk.getFilePath(), elseBlk.getLine());
				throw e;
			}
			
			if(elseBlk.isCondResult(context)){
				break;
			}
		}
		
		
		
		return result;
	}

	public IScriptCondition getCondition() {
		return condition;
	}

	public void setCondition(IScriptCondition condition)
	{
		condition.setCallerSentence(this);
		this.condition = condition;
		
		condition.setFilePath(this.filePath);
	}

	public void exportIntoJDomElement(Element parent) throws AlinousException
	{
		Element selectElement = new Element(IExecutable.TAG_EXECUTABLE);
		selectElement.setAttribute(IExecutable.ATTR_CLASS, this.getClass().getName());
		
		parent.addContent(selectElement);
		
		this.condition.exportIntoJDomElement(selectElement);
		
	}

	public void importFromJDomElement(Element element) throws AlinousException
	{
		Element cond = element.getChild(IScriptCondition.TAG_CONDITION);
		this.condition = JDomConditionFactory.createConditionFromJDomElement(cond);
		
		this.condition.importFromJDomElement(cond);
	}

	public String getName()
	{
		return BLOCK_NAME;
	}
	
	

	public boolean isCondResult(PostContext context)
	{
		return context.getIfResult(this);
	}

	public StepInCandidates getStepInCandidates()
	{
		StepInCandidates candidates = new StepInCandidates();
		
		this.condition.canStepInStatements(candidates);
		
		return candidates;
	}
	
	public void addElseBlock(ElseBlock elseBlock)
	{
		this.elseBlockList.add(elseBlock);
		
	}
	
	public List<ElseBlock> getElseBlocks()
	{
		return this.elseBlockList;
	}
	@Override
	public void setFilePath(String filePath) {
		super.setFilePath(filePath);
		
		this.condition.setFilePath(filePath);
		
		Iterator<ElseBlock> it = this.elseBlockList.iterator();
		while(it.hasNext()){
			ElseBlock elseBlock = it.next();
			
			elseBlock.setFilePath(filePath);
		}
	}
	@Override
	public void getFunctionCall(ScriptCheckContext scContext, List<FunctionCall> call, AlinousScript script)
	{
		super.getFunctionCall(scContext, call, script);
		
		if(this.condition != null){
			this.condition.getFunctionCall(scContext, call, script);
		}
		
		Iterator<ElseBlock> it = this.elseBlockList.iterator();
		while(it.hasNext()){
			ElseBlock elseBlock = it.next();
			
			elseBlock.getFunctionCall(scContext, call, script);
		}
	}
	@Override
	public void checkStaticErrors(ScriptCheckContext scContext,
			List<ScriptError> errorList)
	{
		super.checkStaticErrors(scContext, errorList);
		
		scContext.setCurrentExecutable(this);
		
		if(this.condition != null){
			this.condition.checkStaticErrors(scContext, errorList);
		}
		
		scContext.setCurrentExecutable(null);
		
		Iterator<ElseBlock> it = this.elseBlockList.iterator();
		while(it.hasNext()){
			ElseBlock elseBlock = it.next();
			
			elseBlock.checkStaticErrors(scContext, errorList);
		}
		
		
	}
	
	
}
