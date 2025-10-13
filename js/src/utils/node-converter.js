import { markdownSchema } from "../schema/markdown-schema.js";

export class NodeConverter {
    static constructHeading(headingNode) {
        const specNode = markdownSchema.nodes.heading_spec.create(
            { level: headingNode.attrs.level },
            markdownSchema.text("#".repeat(headingNode.attrs.level) + " ")
        );

        return markdownSchema.nodes.notation_block.create(
            { type: "heading" },
            [specNode, headingNode]
        );
    }
}