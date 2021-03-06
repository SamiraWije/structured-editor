/* {{{ License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */ //}}}

// :indentSize=4:lineSeparator=\n:noTabs=false:tabSize=4:folding=explicit:collapseFolds=0:

package org.mathpiper.builtin.functions.core;

import org.mathpiper.builtin.BuiltinFunction;
import org.mathpiper.exceptions.EvaluationException;
import org.mathpiper.lisp.Environment;

/**
 *
 *  
 */
public class PatchLoad extends BuiltinFunction
{

    public void evaluate(Environment aEnvironment, int aStackTop) throws Exception
    {
        aEnvironment.write("Function not yet implemented : PatchLoad");//TODO FIXME

        throw new EvaluationException("Function not yet supported",-1);
    }
}



/*
%mathpiper_docs,name="PatchLoad",categories="User Functions;Input/Output;Built In"
*CMD PatchLoad --- execute commands between {<?} and {?>} in file
*CORE
*CALL
	PatchLoad(name)

*PARMS

{name} -- string, name of the file to "patch"

*DESC

{PatchLoad} loads in a file and outputs the contents to the current
output. The file can contain blocks delimited by {<?} and {?>}
(meaning "MathPiper Begin" and "MathPiper End"). The piece of text between
such delimiters is treated as a separate file with MathPiper instructions,
which is then loaded and executed. All output of write statements
in that block will be written to the same current output.

This is similar to the way PHP works. You can have a static text file
with dynamic content generated by MathPiper.

*SEE PatchString, Load
%/mathpiper_docs
*/